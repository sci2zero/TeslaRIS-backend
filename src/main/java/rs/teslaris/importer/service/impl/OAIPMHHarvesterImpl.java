package rs.teslaris.importer.service.impl;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.oaipmh.common.Metadata;
import rs.teslaris.core.model.oaipmh.common.OAIPMHResponse;
import rs.teslaris.core.model.oaipmh.common.ResumptionToken;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NetworkException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.service.interfaces.OAIPMHHarvester;
import rs.teslaris.importer.utility.CommonHarvestUtility;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.DeepObjectMerger;
import rs.teslaris.importer.utility.HarvestProgressReport;
import rs.teslaris.importer.utility.oaipmh.OAIPMHHarvestConfigurationLoader;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAIPMHHarvesterImpl implements OAIPMHHarvester {

    private static final int MAX_RESTART_NUMBER = 10;

    private final MongoTemplate mongoTemplate;

    private final RestTemplateProvider restTemplateProvider;

    private final PersonService personService;

    private final PersonIndexRepository personIndexRepository;


    @Override
    public void harvest(String sourceName, LocalDate startDate, LocalDate endDate, Integer userId) {
        var sourceConfiguration = getSourceConfiguration(sourceName);

        var adminUserIds = CommonImportUtility.getAdminUserIds();

        var endpoint = constructOAIPMHEndpoint(sourceConfiguration, startDate, endDate);
        var restTemplate = restTemplateProvider.provideRestTemplate();
        var identifyingDataset = constructIdentifyingDatasetName(sourceConfiguration);
        var harvestProgressReport = getProgressReport(identifyingDataset, userId);

        if (Objects.nonNull(harvestProgressReport)) {
            endpoint = sourceConfiguration.baseUrl() + "?verb=ListRecords&resumptionToken=" +
                harvestProgressReport.getResumptionToken();
        }

        var newEntriesCount = new HashMap<Integer, Integer>();
        int restartCount = 0;

        while (true) {
            try {
                ResponseEntity<String> responseEntity =
                    restTemplate.getForEntity(endpoint, String.class);

                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    String responseBody = responseEntity.getBody();
                    var optionalOaiPmhResponse = parseResponse(responseBody);

                    if (optionalOaiPmhResponse.isEmpty()) {
                        break;
                    }

                    var parsedResponse = handleOAIPMHResponse(optionalOaiPmhResponse.get());
                    processParsedRecords(parsedResponse.a, sourceConfiguration, newEntriesCount,
                        userId, adminUserIds);

                    if (parsedResponse.b.isEmpty() || parsedResponse.b.get().getValue().isBlank()) {
                        break;
                    }

                    endpoint =
                        sourceConfiguration.baseUrl() + "?verb=ListRecords&resumptionToken=" +
                            parsedResponse.b.get().getValue();

                    updateProgressReport(identifyingDataset, parsedResponse.b.get().getValue(),
                        userId);
                } else {
                    log.error("OAI-PMH request failed with response code: " +
                        responseEntity.getStatusCode());
                }
            } catch (Exception e) {
                if (restartCount == MAX_RESTART_NUMBER) {
                    var message =
                        "Harvest did not complete because host (" + sourceConfiguration.baseUrl() +
                            ") keeps crashing. Manual restart required.";
                    log.error(message);
                    throw new NetworkException(message);
                }

                restartCount += 1;

                log.warn("No route to host for endpoint: " + endpoint + " - Restarting " +
                    restartCount + " of " + MAX_RESTART_NUMBER);
            }
        }

        deleteProgressReport(identifyingDataset, userId);
    }

    @Override
    public List<String> getSources() {
        return OAIPMHHarvestConfigurationLoader.getAllSourceNames();
    }

    private OAIPMHHarvestConfigurationLoader.Source getSourceConfiguration(String sourceName) {
        var config = OAIPMHHarvestConfigurationLoader.getSourceConfigurationByName(sourceName);
        if (Objects.isNull(config)) {
            throw new NotFoundException("OAIPMH source with given name does not exist.");
        }
        return config;
    }

    private void processParsedRecords(
        List<Metadata> records,
        OAIPMHHarvestConfigurationLoader.Source sourceConfiguration,
        Map<Integer, Integer> newEntriesCount,
        Integer userId,
        Set<Integer> adminUserIds
    ) {
        try {
            Class<?> converterClass = Class.forName(
                "rs.teslaris.importer.model.converter.harvest." +
                    sourceConfiguration.converterClass());
            Class<?> responseClass = Class.forName(
                "rs.teslaris.core.model." + sourceConfiguration.responseObjectClass());
            var method = converterClass.getMethod("toCommonImportModel", responseClass);

            for (var record : records) {
                processSingleRecord(record, method, responseClass, newEntriesCount, userId,
                    adminUserIds);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log.error(
                "SERIOUS: Invalid converter ({}) or response ({}) class specified in OAI-PMH harvest.",
                sourceConfiguration.converterClass(), sourceConfiguration.responseObjectClass());
        }
    }

    @SuppressWarnings("unchecked")
    private void processSingleRecord(
        Metadata record,
        Method method,
        Class<?> responseClass,
        Map<Integer, Integer> newEntriesCount,
        Integer userId,
        Set<Integer> adminUserIds
    ) {
        var publication = record.getPublication();
        if (!responseClass.isInstance(publication)) {
            throw new IllegalArgumentException(
                "Publication is not an instance of specified class.");
        }

        try {
            ((Optional<DocumentImport>) method.invoke(null, responseClass.cast(publication)))
                .ifPresent(documentImport -> {
                    bindImportUsersForAll(documentImport);
                    if (documentImport.getImportUsersId().isEmpty()) {
                        return;
                    }

                    var existingImport =
                        CommonImportUtility.findExistingImport(documentImport.getIdentifier());
                    if (Objects.isNull(existingImport)) {
                        existingImport =
                            CommonImportUtility.findImportByDOIOrMetadata(documentImport);
                        if (Objects.nonNull(existingImport)) {
                            // Probably imported before from other sources, which have higher priorities
                            // perform metadata enrichment, if possible
                            DeepObjectMerger.deepMerge(existingImport, documentImport);
                            mongoTemplate.save(existingImport, "documentImports");
                            return;
                        }
                    }

                    var embedding = CommonImportUtility.generateEmbedding(documentImport);
                    if (DeduplicationUtil.isDuplicate(existingImport, embedding, documentImport)) {
                        return;
                    }

                    if (Objects.nonNull(embedding)) {
                        documentImport.setEmbedding(DeduplicationUtil.toDoubleList(embedding));
                    }

                    documentImport.getImportUsersId().addAll(adminUserIds);
                    newEntriesCount.merge(userId, 1, Integer::sum);

                    CommonHarvestUtility.updateContributorEntryCount(documentImport,
                        documentImport.getContributions().stream()
                            .map(c -> c.getPerson().getOrcid()).toList(),
                        newEntriesCount,
                        personService);

                    mongoTemplate.save(documentImport, "documentImports");
                });
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("SERIOUS: Invalid converter invocation.", e);
        }
    }

    private void bindImportUsersForAll(DocumentImport documentImport) {
        documentImport.getContributions().forEach(authorship -> {
            if (Objects.nonNull(authorship.getPerson().getOrcid()) &&
                !authorship.getPerson().getOrcid().isBlank()) {
                var researcher =
                    personService.findUserByIdentifier(authorship.getPerson().getOrcid());
                if (researcher.isPresent()) {
                    documentImport.getImportUsersId().add(researcher.get().getId());
                    personIndexRepository.findByDatabaseId(
                            personService.getPersonIdForUserId(researcher.get().getId()))
                        .ifPresent(personIndex -> {
                            documentImport.getImportInstitutionsId()
                                .addAll(personIndex.getEmploymentInstitutionsIdHierarchy());
                        });
                } else {
                    bindImportUsers(documentImport, authorship);
                }
            } else {
                bindImportUsers(documentImport, authorship);
            }
        });
    }

    public Optional<OAIPMHResponse> parseResponse(String xml) {
        try {
            var jaxbContext = JAXBContext.newInstance(OAIPMHResponse.class);

            var saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            saxParserFactory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                Boolean.FALSE);
            saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",
                Boolean.TRUE);
            saxParserFactory.setValidating(Boolean.FALSE);

            var xmlReader = saxParserFactory.newSAXParser().getXMLReader();
            var inputSource = new InputSource(new StringReader(xml));
            var source = new SAXSource(xmlReader, inputSource);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return Optional.of(
                (OAIPMHResponse) jaxbUnmarshaller.unmarshal(source.getInputSource()));
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            log.error("Parsing OAI-PMH response failed. Reason: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Pair<ArrayList<Metadata>, Optional<ResumptionToken>> handleOAIPMHResponse(
        OAIPMHResponse oaiPmhResponse) {
        var parsedRecords = new ArrayList<Metadata>();

        if (Objects.isNull(oaiPmhResponse.getListRecords())) {
            return new Pair<>(parsedRecords, Optional.empty());
        }

        var records = oaiPmhResponse.getListRecords().getRecords();
        records.forEach(
            record -> {
                if (Objects.nonNull(record.getHeader().getStatus()) &&
                    record.getHeader().getStatus().equalsIgnoreCase("deleted")) {
                    // TODO: should deleted records be removed from our db?
                    return;
                }
                var metadata = record.getMetadata();
                parsedRecords.add(metadata);
            });

        var resumptionToken = oaiPmhResponse.getListRecords().getResumptionToken();
        if (Objects.isNull(resumptionToken) || Objects.isNull(resumptionToken.getValue())) {
            return new Pair<>(parsedRecords, Optional.empty());
        }

        return new Pair<>(parsedRecords, Optional.of(resumptionToken));
    }

    private String constructOAIPMHEndpoint(OAIPMHHarvestConfigurationLoader.Source sourceConfig,
                                           LocalDate from, LocalDate until) {
        return sourceConfig.baseUrl() + "?verb=ListRecords&set=" + sourceConfig.dataset() +
            "&metadataPrefix=" + sourceConfig.metadataFormat() + "&from=" + from.toString() +
            "&until=" + until.toString();
    }

    private String constructIdentifyingDatasetName(
        OAIPMHHarvestConfigurationLoader.Source sourceConfig) {
        return "HARVEST_" + sourceConfig.sourceName() + "_" + sourceConfig.dataset();
    }

    @Nullable
    private HarvestProgressReport getProgressReport(String datasetName, Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("dataset").is(datasetName))
            .addCriteria(Criteria.where("userId").is(userId));
        return mongoTemplate.findOne(query, HarvestProgressReport.class);
    }

    private void updateProgressReport(String datasetName, String resumptionToken,
                                      Integer userId) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(datasetName))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, HarvestProgressReport.class);

        mongoTemplate.save(new HarvestProgressReport(resumptionToken, userId, datasetName));
    }

    private void deleteProgressReport(String datasetName, Integer userId) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("dataset").is(datasetName))
            .addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(deleteQuery, HarvestProgressReport.class);
    }

    private void bindImportUsers(DocumentImport documentImport,
                                 PersonDocumentContribution authorship) {
        personService.findPeopleByNameAndEmployment(Arrays.stream(
                    authorship.getPerson().getName().toString().split(" "))
                .toList(), Pageable.unpaged(), false, null, false)
            .forEach(person -> {
                if (Objects.nonNull(person.getUserId()) && person.getUserId() > 0) {
                    documentImport.getImportUsersId().add(person.getUserId());
                }

                documentImport.getImportInstitutionsId()
                    .addAll(person.getEmploymentInstitutionsIdHierarchy());
            });
    }
}
