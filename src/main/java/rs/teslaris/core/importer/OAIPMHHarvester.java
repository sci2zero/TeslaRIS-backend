package rs.teslaris.core.importer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rs.teslaris.core.importer.common.OAIPMHDataSet;
import rs.teslaris.core.importer.common.OAIPMHResponse;
import rs.teslaris.core.importer.common.ResumptionToken;
import rs.teslaris.core.importer.converter.event.EventConverter;
import rs.teslaris.core.importer.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.importer.converter.person.PersonConverter;
import rs.teslaris.core.importer.event.Event;
import rs.teslaris.core.importer.organisationunit.OrgUnit;
import rs.teslaris.core.importer.person.Person;
import rs.teslaris.core.importer.utility.CreatorMethod;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAIPMHHarvester {

    private final String BASE_URL = "https://cris.uns.ac.rs/OAIHandlerTeslaRIS";

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitConverter organisationUnitConverter;

    private final PersonConverter personConverter;

    private final EventConverter eventConverter;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final InvolvementService involvementService;

    private final ConferenceService conferenceService;

    @Value("${ssl.trust-store}")
    private String trustStorePath;

    @Value("${ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:80}")
    private Integer proxyPort;


    public void harvest(OAIPMHDataSet requestDataSet) {
        String endpoint = constructOAIPMHEndpoint(requestDataSet.getStringValue());
        var restTemplate = constructRestTemplate();

        while (true) {
            ResponseEntity<String> responseEntity =
                restTemplate.getForEntity(endpoint, String.class);
            if (responseEntity.getStatusCodeValue() == 200) {
                String responseBody = responseEntity.getBody();
                var optionalOaiPmhResponse = parseResponse(responseBody);
                if (optionalOaiPmhResponse.isEmpty()) {
                    break;
                }

                var optionalResumptionToken =
                    handleOAIPMHResponse(requestDataSet, optionalOaiPmhResponse.get());
                if (optionalResumptionToken.isEmpty()) {
                    break;
                }

                endpoint =
                    BASE_URL + "?verb=ListRecords&resumptionToken=" +
                        optionalResumptionToken.get().getValue();
            } else {
                log.error("OAI-PMH request failed with response code: " +
                    responseEntity.getStatusCodeValue());
            }
        }
    }

    public void loadRecords(OAIPMHDataSet requestDataSet, boolean performIndex) {
        int batchSize = 10;
        int page = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var pageable = PageRequest.of(page, batchSize);
            var query = new Query().with(pageable);

            switch (requestDataSet) {
                case ORGANISATION_UNITS:
                    hasNextPage = loadBatch(OrgUnit.class, organisationUnitConverter,
                        organisationUnitService::createOrganisationUnit, query, performIndex,
                        batchSize);
                    break;
                case PERSONS:
                    hasNextPage = loadBatch(Person.class, personConverter,
                        personService::createPersonWithBasicInfo, query, performIndex, batchSize);
                    break;
                case EVENTS:
                    hasNextPage = loadBatch(Event.class, eventConverter,
                        conferenceService::createConference, query, performIndex, batchSize);
                    break;
            }

            page++;
        }

        handleDataRelations(requestDataSet);
    }

    private <T, D, R> boolean loadBatch(Class<T> entityClass, RecordConverter<T, D> converter,
                                        CreatorMethod<D, R> creatorMethod, Query query,
                                        boolean performIndex, int batchSize) {
        List<T> batch = mongoTemplate.find(query, entityClass);
        batch.forEach(record -> {
            D creationDTO = converter.toDTO(record);
            creatorMethod.apply(creationDTO, performIndex);
        });
        return batch.size() == batchSize;
    }

    private void handleDataRelations(OAIPMHDataSet requestDataSet) {
        int batchSize = 10;
        int page = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var pageable = PageRequest.of(page, batchSize);
            var query = new Query().with(pageable);

            switch (requestDataSet) {
                case ORGANISATION_UNITS:
                    List<OrgUnit> orgUnitBatch = mongoTemplate.find(query, OrgUnit.class);
                    orgUnitBatch.forEach((orgUnit) -> {
                        var creationDTO = organisationUnitConverter.toRelationDTO(orgUnit);
                        creationDTO.ifPresent(
                            organisationUnitService::createOrganisationUnitsRelation);
                    });
                    page++;
                    hasNextPage = orgUnitBatch.size() == batchSize;
                    break;
                case PERSONS:
                    List<Person> personBatch = mongoTemplate.find(query, Person.class);
                    personBatch.forEach((person) -> {
                        var savedPerson = personService.findPersonByOldId(
                            OAIPMHParseUtility.parseBISISID(person.getId()));
                        if (!Objects.nonNull(person.getAffiliation()) &&
                            Objects.nonNull(savedPerson)) {
                            return;
                        }
                        person.getAffiliation().getOrgUnits().forEach(((affiliation) -> {
                            var creationDTO =
                                personConverter.toPersonEmployment(affiliation);
                            creationDTO.ifPresent(employmentDTO -> involvementService.addEmployment(
                                savedPerson.getId(), employmentDTO));
                        }));
                    });
                    page++;
                    hasNextPage = personBatch.size() == batchSize;
                    break;
                default:
                    hasNextPage = false;
                    break;
            }
        }
    }

    private Optional<ResumptionToken> handleOAIPMHResponse(OAIPMHDataSet requestDataSet,
                                                           OAIPMHResponse oaiPmhResponse) {
        if (oaiPmhResponse.getListRecords() == null) {
            return Optional.empty();
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
                switch (requestDataSet) {
                    case EVENTS:
                        mongoTemplate.save(metadata.getEvent());
                        break;
                    case PATENTS:
                        mongoTemplate.save(metadata.getPatent());
                        break;
                    case PERSONS:
                        mongoTemplate.save(metadata.getPerson());
                        break;
                    case PRODUCTS:
                        mongoTemplate.save(metadata.getProduct());
                        break;
                    case PUBLICATIONS:
                        mongoTemplate.save(metadata.getPublication());
                        break;
                    case ORGANISATION_UNITS:
                        mongoTemplate.save(metadata.getOrgUnit());
                        break;
                }
            });
        var resumptionToken = oaiPmhResponse.getListRecords().getResumptionToken();
        if (resumptionToken.getValue() == null) {
            return Optional.empty();
        }

        return Optional.of(resumptionToken);
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

    private RestTemplate constructRestTemplate() {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;

        SSLContext sslContext;
        try (InputStream truststoreInputStream = new FileInputStream(trustStorePath)) {
            var truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore.load(truststoreInputStream, trustStorePassword.toCharArray());

            sslContext = SSLContexts.custom()
                .loadTrustMaterial(truststore, acceptingTrustStrategy)
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException |
                 CertificateException | IOException e) {
            log.error("Rest template construction failed. Reason:\n" + e.getMessage());
            throw new CantConstructRestTemplateException(
                "Unable to establish secure connection to remote host.");
        }

        var connectionSocketFactory =
            new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        var httpClient = HttpClients.custom().setSSLSocketFactory(connectionSocketFactory);

        if (!proxyHost.isEmpty()) {
            httpClient.setProxy(new HttpHost(proxyHost, proxyPort));
        }

        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient.build());

        return new RestTemplate(requestFactory);
    }

    private String constructOAIPMHEndpoint(String set) {
        return BASE_URL + "?verb=ListRecords&set=" + set + "&metadataPrefix=oai_cerif_openaire";
    }
}
