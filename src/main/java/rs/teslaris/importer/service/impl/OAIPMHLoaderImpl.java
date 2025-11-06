package rs.teslaris.importer.service.impl;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentIdentifierUpdateDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitWizardDTO;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.model.oaipmh.common.HasOldId;
import rs.teslaris.core.model.oaipmh.event.Event;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnitRelation;
import rs.teslaris.core.model.oaipmh.patent.Patent;
import rs.teslaris.core.model.oaipmh.person.Person;
import rs.teslaris.core.model.oaipmh.product.Product;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.importer.dto.RemainingRecordsCountResponseDTO;
import rs.teslaris.importer.model.converter.load.event.EventConverter;
import rs.teslaris.importer.model.converter.load.institution.OrganisationUnitConverter;
import rs.teslaris.importer.model.converter.load.person.ImportPersonConverter;
import rs.teslaris.importer.model.converter.load.publication.DissertationConverter;
import rs.teslaris.importer.model.converter.load.publication.JournalConverter;
import rs.teslaris.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.MagistrateConverter;
import rs.teslaris.importer.model.converter.load.publication.MonographConverter;
import rs.teslaris.importer.model.converter.load.publication.MonographPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.PatentConverter;
import rs.teslaris.importer.model.converter.load.publication.ProceedingsConverter;
import rs.teslaris.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.ProductConverter;
import rs.teslaris.importer.service.interfaces.OAIPMHLoader;
import rs.teslaris.importer.utility.CreatorMethod;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.ProgressReportUtility;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAIPMHLoaderImpl implements OAIPMHLoader {

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitConverter organisationUnitConverter;

    private final ImportPersonConverter importPersonConverter;

    private final EventConverter eventConverter;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final InvolvementService involvementService;

    private final ConferenceService conferenceService;

    private final JournalPublicationService journalPublicationService;

    private final JournalPublicationConverter journalPublicationConverter;

    private final ProceedingsPublicationService proceedingsPublicationService;

    private final ProceedingsPublicationConverter proceedingsPublicationConverter;

    private final ProceedingsConverter proceedingsConverter;

    private final JournalConverter journalConverter;

    private final JournalService journalService;

    private final ProceedingsService proceedingsService;

    private final PatentConverter patentConverter;

    private final PatentService patentService;

    private final SoftwareService softwareService;

    private final ProductConverter productConverter;

    private final DissertationConverter dissertationConverter;

    private final MagistrateConverter magistrateConverter;

    private final ThesisService thesisService;

    private final MonographConverter monographConverter;

    private final MonographService monographService;

    private final MonographPublicationConverter monographPublicationConverter;

    private final MonographPublicationService monographPublicationService;

    private final DocumentPublicationService documentPublicationService;


    @Override
    @SuppressWarnings("unchecked")
    public <R> R loadRecordsWizard(DataSet requestDataSet, Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("importUserId").in(userId));
        query.addCriteria(Criteria.where("loaded").is(false));

        var progressReport =
            ProgressReportUtility.getProgressReport(requestDataSet, userId, null, mongoTemplate);
        if (progressReport != null) {
            query.addCriteria(
                Criteria.where("oldId").gte(progressReport.getLastLoadedIdentifier()));
        } else {
            query.addCriteria(Criteria.where("oldId").gte(""));
        }
        query.limit(1);

        switch (requestDataSet) {
            case PERSONS:
                return (R) findAndConvertEntity(Person.class, importPersonConverter,
                    DataSet.PERSONS, query, userId);
            case EVENTS:
                return (R) findAndConvertEntity(Event.class, eventConverter, DataSet.EVENTS,
                    query, userId);
            case PATENTS:
                return (R) findAndConvertEntity(Patent.class, patentConverter,
                    DataSet.PATENTS, query, userId);
            case PRODUCTS:
                return (R) findAndConvertEntity(Product.class, productConverter,
                    DataSet.PRODUCTS, query, userId);
            case CONFERENCE_PROCEEDINGS:
                query.addCriteria(Criteria.where("type").regex("c_f744$"));
                return (R) findAndConvertEntity(Publication.class, proceedingsConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case JOURNALS:
                query.addCriteria(Criteria.where("type").regex("c_0640"));
                return (R) findAndConvertEntity(Publication.class, journalConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case RESEARCH_ARTICLES:
                query.addCriteria(Criteria.where("type").regex("c_2df8fbb1"));
                query.addCriteria(Criteria.where("type").regex("c_3e5a"));
                return (R) findAndConvertEntity(Publication.class, journalPublicationConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case CONFERENCE_PUBLICATIONS:
                query.addCriteria(Criteria.where("type").regex("c_5794"));
                query.addCriteria(Criteria.where("type").regex("c_c94f"));
                return (R) findAndConvertEntity(Publication.class, proceedingsPublicationConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case PHD_THESES:
                query.addCriteria(Criteria.where("type").regex("c_db06"));
                return (R) findAndConvertEntity(Publication.class, dissertationConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case MR_THESES:
                query.addCriteria(Criteria.where("type").regex("c_46ec"));
                return (R) findAndConvertEntity(Publication.class, magistrateConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case MONOGRAPHS:
                query.addCriteria(Criteria.where("type").regex("c_2f33"));
                return (R) findAndConvertEntity(Publication.class, monographConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case MONOGRAPH_PUBLICATIONS:
                query.addCriteria(Criteria.where("type").regex("c_3248"));
                return (R) findAndConvertEntity(Publication.class, monographPublicationConverter,
                    DataSet.PUBLICATIONS, query, userId);
            case ORGANISATION_UNITS:
                var orgUnit = (OrganisationUnitWizardDTO) findAndConvertEntity(OrgUnit.class,
                    organisationUnitConverter, DataSet.ORGANISATION_UNITS, query, userId);
                if (Objects.nonNull(orgUnit) &&
                    Objects.nonNull(orgUnit.getSuperOrganisationUnitId())) {
                    mongoTemplate.save(
                        new OrgUnitRelation(orgUnit.getOldId(),
                            orgUnit.getSuperOrganisationUnitId()));
                }
                return (R) orgUnit;
        }

        return null;
    }

    @Override
    public <R> R loadSkippedRecordsWizard(DataSet requestDataSet, Integer userId) {
        ProgressReportUtility.resetProgressReport(requestDataSet, userId, null, mongoTemplate);
        return loadRecordsWizard(requestDataSet, userId);
    }

    @Override
    public void skipRecord(DataSet requestDataSet, Integer userId) {
        var entityClass = DataSet.getClassForValue(requestDataSet.getStringValue());

        var progressReport =
            ProgressReportUtility.getProgressReport(requestDataSet, userId, null, mongoTemplate);
        Query nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("importUserId").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("loaded").is(false));
        nextRecordQuery.addCriteria(
            Criteria.where("oldId").gt(progressReport.getLastLoadedIdentifier()));

        var nextRecord = mongoTemplate.findOne(nextRecordQuery, entityClass);
        if (Objects.nonNull(nextRecord)) {
            Method getIdMethod;
            try {
                getIdMethod = entityClass.getMethod("getOldId");
                progressReport.setLastLoadedIdentifier((String) getIdMethod.invoke(nextRecord));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                return;
            }
        } else {
            progressReport.setLastLoadedIdentifier("");
        }

        ProgressReportUtility.deleteProgressReport(requestDataSet, userId, null, mongoTemplate);
        mongoTemplate.save(progressReport);
    }

    @Override
    public void markRecordAsLoaded(DataSet requestDataSet, Integer userId) {
        var progressReport =
            ProgressReportUtility.getProgressReport(requestDataSet, userId, null, mongoTemplate);
        Query query = new Query();
        query.addCriteria(Criteria.where("oldId").is(progressReport.getLastLoadedIdentifier()));
        query.addCriteria(Criteria.where("importUserId").in(userId));

        var entityClass = DataSet.getClassForValue(requestDataSet.getStringValue());
        var record = mongoTemplate.findOne(query, entityClass);

        if (Objects.nonNull(record)) {
            Method setLoadedMethod;
            try {
                setLoadedMethod = entityClass.getMethod("setLoaded", Boolean.class);
                setLoadedMethod.invoke(record, true);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                return;
            }
            mongoTemplate.save(record);
        }
    }

    @Override
    public RemainingRecordsCountResponseDTO countRemainingDocumentsForLoading(Integer userId) {
        var countResponse = new RemainingRecordsCountResponseDTO();

        var countQuery = new Query();
        countQuery.addCriteria(Criteria.where("loaded").is(false));
        countQuery.addCriteria(Criteria.where("importUserId").in(userId));

        countResponse.setPatentCount(mongoTemplate.count(countQuery, Patent.class));
        countResponse.setPersonCount(mongoTemplate.count(countQuery, Person.class));
        countResponse.setEventCount(mongoTemplate.count(countQuery, Event.class));
        countResponse.setProductCount(mongoTemplate.count(countQuery, Product.class));
        countResponse.setOuCount(mongoTemplate.count(countQuery, OrgUnit.class));
        countResponse.setResearchArticleCount(mongoTemplate.count(countQuery, Publication.class));
        countResponse.setProceedingsPublicationCount(
            mongoTemplate.count(countQuery, Publication.class));
        countResponse.setJournalCount(mongoTemplate.count(countQuery, Publication.class));
        countResponse.setProceedingsCount(mongoTemplate.count(countQuery, Publication.class));

        return countResponse;
    }

    @Nullable
    private <T, D> D findAndConvertEntity(Class<T> entityClass, RecordConverter<T, D> converter,
                                          DataSet requestDataSet, Query query,
                                          Integer userId) {
        var entity = mongoTemplate.findOne(query, entityClass);

        if (Objects.nonNull(entity)) {
            Method getIdMethod;
            try {
                getIdMethod = entityClass.getMethod("getOldId");
            } catch (NoSuchMethodException e) {
                return null;
            }
            try {
                ProgressReportUtility.updateProgressReport(requestDataSet,
                    (String) getIdMethod.invoke(entity), (String) getIdMethod.invoke(entity),
                    userId, null, mongoTemplate);
            } catch (IllegalAccessException | InvocationTargetException e) {
                return null;
            }
            return converter.toDTO(entity);
        }
        return null;
    }

    @Override
    public void loadRecordsAuto(DataSet requestDataSet, boolean performIndex,
                                Integer userId) {
        int batchSize = 500;
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
                    hasNextPage = loadBatch(Person.class, importPersonConverter,
                        personService::importPersonWithBasicInfo, query, performIndex, batchSize);
                    break;
                case EVENTS:
                    hasNextPage = loadBatch(Event.class, eventConverter,
                        conferenceService::createConference, query, performIndex, batchSize);
                    break;
                case PUBLICATIONS:
                    var criteria = new Criteria().orOperator(
                        Criteria.where("type").regex("c_f744$"),
                        Criteria.where("type").regex("c_0640$"),
                        Criteria.where("type").regex("c_2f33$")
                    );
                    var batch = mongoTemplate.find(query.addCriteria(criteria), Publication.class);
                    batch.forEach(record -> {
                        if (record.getType()
                            .endsWith("c_f744")) { // COAR type: conference proceedings
                            var creationDTO = proceedingsConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                try {
                                    proceedingsService.createProceedings(creationDTO, performIndex);
                                } catch (Exception e) {
                                    log.warn(
                                        "Skipped loading object of type 'PROCEEDINGS' with id '{}'. Reason: '{}'.",
                                        record.getOldId(), e.getMessage());
                                    if (e.getMessage().endsWith("isbnExistsError")) {
                                        var potentialMatch =
                                            proceedingsService.findProceedingsByIsbn(
                                                creationDTO.getEISBN(), creationDTO.getPrintISBN());
                                        if (Objects.nonNull(potentialMatch)) {
                                            proceedingsService.addOldId(potentialMatch.getId(),
                                                creationDTO.getOldId());
                                            log.info(
                                                "Successfully merged PROCEEDINGS '{}' using ISBN ({}, {}).",
                                                record.getOldId(), creationDTO.getEISBN(),
                                                creationDTO.getPrintISBN());
                                        }
                                    }
                                }
                            }
                        } else if (record.getType().endsWith("c_0640")) { // COAR type: journal
                            var creationDTO = journalConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                try {
                                    journalService.createJournal(creationDTO, performIndex);
                                } catch (Exception e) {
                                    log.warn(
                                        "Skipped loading object of type 'JOURNAL' with id '{}'. Reason: '{}'.",
                                        record.getOldId(), e.getMessage());
                                    if (e.getMessage().endsWith("issnExistsError")) {
                                        var potentialMatch =
                                            journalService.readJournalByIssn(creationDTO.getEissn(),
                                                creationDTO.getPrintISSN());
                                        if (Objects.nonNull(potentialMatch)) {
                                            journalService.addOldId(potentialMatch.getDatabaseId(),
                                                creationDTO.getOldId());
                                            log.info(
                                                "Successfully merged JOURNAL '{}' using ISSN ({}, {}).",
                                                record.getOldId(), creationDTO.getEissn(),
                                                creationDTO.getPrintISSN());
                                        }
                                    }
                                }
                            }
                        } else if (record.getType().endsWith("c_2f33")) { // COAR type: monograph
                            var creationDTO = monographConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                try {
                                    monographService.createMonograph(creationDTO, performIndex);
                                } catch (Exception e) {
                                    log.warn(
                                        "Skipped loading object of type 'MONOGRAPH' with id '{}'. Reason: '{}'.",
                                        record.getOldId(), e.getMessage());
                                    if (e.getMessage().endsWith("isbnExistsError")) {
                                        var potentialMatch = monographService.findMonographByIsbn(
                                            creationDTO.getEisbn(), creationDTO.getPrintISBN());
                                        if (Objects.nonNull(potentialMatch)) {
                                            monographService.addOldId(potentialMatch.getId(),
                                                creationDTO.getOldId());
                                            log.info(
                                                "Successfully merged MONOGRAPH '{}' using ISSN ({}, {}).",
                                                record.getOldId(), creationDTO.getEisbn(),
                                                creationDTO.getPrintISBN());
                                        }
                                    }
                                }
                            }
                        }
                    });
                    hasNextPage = batch.size() == batchSize;
                    break;
                case PATENTS:
                    hasNextPage = loadBatch(Patent.class, patentConverter,
                        patentService::createPatent, query, performIndex, batchSize);
                    break;
                case PRODUCTS:
                    hasNextPage = loadBatch(Product.class, productConverter,
                        softwareService::createSoftware, query, performIndex, batchSize);
                    break;
            }
            page++;
        }

        handleDataRelations(requestDataSet, performIndex);
    }

    private <T extends HasOldId, D, R> boolean loadBatch(Class<T> entityClass,
                                                         RecordConverter<T, D> converter,
                                                         CreatorMethod<D, R> creatorMethod,
                                                         Query query,
                                                         boolean performIndex, int batchSize) {
        List<T> batch = mongoTemplate.find(query, entityClass);
        batch.forEach(record -> {
            D creationDTO = converter.toDTO(record);
            try {
                creatorMethod.apply(creationDTO, performIndex);
            } catch (Exception e) {
                log.warn("Skipped loading object of type '{}' with id '{}'. Reason: '{}'.",
                    creationDTO.getClass(), record.getOldId(), e.getMessage());
                if (entityClass.equals(Person.class) &&
                    creationDTO instanceof ImportPersonDTO importDTO) {
                    Map<String, Function<ImportPersonDTO, String>> identifierResolvers = Map.of(
                        "scopusAuthorIdExistsError", ImportPersonDTO::getScopusAuthorId,
                        "orcidIdExistsError", ImportPersonDTO::getOrcid,
                        "apvntExistsError", ImportPersonDTO::getApvnt
                    );

                    Optional.ofNullable(identifierResolvers.get(e.getMessage()))
                        .map(resolver -> resolver.apply(importDTO))
                        .flatMap(personService::findPersonByIdentifier)
                        .ifPresent(person -> {
                            personService.addOldId(person.getId(), importDTO.getOldId());
                            log.info(
                                "Successfully merged PERSON '{}' using identifiers ({}, {}).",
                                record.getOldId(), importDTO.getOrcid(),
                                importDTO.getScopusAuthorId());
                        });
                }
            }
        });
        return batch.size() == batchSize;
    }

    private void handleDataRelations(DataSet requestDataSet, boolean performIndex) {
        int batchSize = 500;
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
                            OAIPMHParseUtility.parseBISISID(person.getOldId()));
                        if (Objects.isNull(person.getAffiliation()) ||
                            Objects.isNull(savedPerson)) {
                            return;
                        }
                        if (Objects.nonNull(person.getAffiliation().getOrgUnits())) {
                            FunctionalUtil.forEachWithCounter(person.getAffiliation().getOrgUnits(),
                                (i, affiliation) -> {
                                    var creationDTO =
                                        importPersonConverter.toPersonEmployment(person,
                                            affiliation);
                                    creationDTO.forEach(
                                        employmentDTO -> involvementService.addEmployment(
                                            savedPerson.getId(), employmentDTO));
                                });
                        } else {
                            log.info("Migrated PERSON '{}' nas no present affiliations.",
                                person.getPersonName().toString());
                        }
                    });
                    page++;
                    hasNextPage = personBatch.size() == batchSize;
                    break;
                case PUBLICATIONS:
                    var criteria = new Criteria().orOperator(
                        Criteria.where("type").regex("c_2df8fbb1$"),
                        Criteria.where("type").regex("c_5794$"),
                        Criteria.where("type").regex("c_c94f$"),
                        Criteria.where("type").regex("c_db06$"),
                        Criteria.where("type").regex("c_3248$"),
                        Criteria.where("type").regex("c_46ec$"),
                        Criteria.where("type").regex("c_3e5a$")
                    );
                    var publicationBatch =
                        mongoTemplate.find(query.addCriteria(criteria), Publication.class);
                    publicationBatch.forEach(record -> {
                        if (record.getType().endsWith("c_2df8fbb1") || record.getType().endsWith(
                            "c_3e5a")) { // COAR type: research article, contribution to journal
                            var creationDTO = journalPublicationConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                try {
                                    journalPublicationService.createJournalPublication(creationDTO,
                                        performIndex);
                                } catch (Exception e) {
                                    log.warn(
                                        "Skipped loading object of type 'JOURNAL_PUBLICATION' with id '{}'. Reason: '{}'.",
                                        record.getOldId(), e.getMessage());
                                }
                            }
                        } else if (
                            record.getType().endsWith("c_5794") || record.getType().endsWith(
                                "c_c94f")) { // COAR type: conference paper, conference output
                            var creationDTO = proceedingsPublicationConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                saveWithDuplicateCheck(
                                    creationDTO,
                                    record.getOldId(),
                                    performIndex,
                                    proceedingsPublicationService::createProceedingsPublication,
                                    "PROCEEDINGS_PUBLICATION",
                                    documentPublicationService);
                            }
                        } else if (record.getType().endsWith("c_3248")) { // COAR type: book part
                            var creationDTO = monographPublicationConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                saveWithDuplicateCheck(
                                    creationDTO,
                                    record.getOldId(),
                                    performIndex,
                                    monographPublicationService::createMonographPublication,
                                    "MONOGRAPH_PUBLICATION",
                                    documentPublicationService);
                            }
                        } else if (record.getType()
                            .endsWith("c_db06")) { // COAR type: dissertation (thesis)
                            var creationDTO = dissertationConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                try {
                                    thesisService.createThesis(creationDTO, performIndex);
                                } catch (Exception e) {
                                    log.warn(
                                        "Skipped loading object of type 'THESIS' (PHD) with id '{}'. Reason: '{}'.",
                                        record.getOldId(), e.getMessage());
                                }
                            }
                        } else if (record.getType().endsWith("c_46ec")) { // COAR type: MR (thesis)
                            var creationDTO = magistrateConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                try {
                                    thesisService.createThesis(creationDTO, performIndex);
                                } catch (Exception e) {
                                    log.warn(
                                        "Skipped loading object of type 'THESIS' (MR) with id '{}'. Reason: '{}'.",
                                        record.getOldId(), e.getMessage());
                                }
                            }
                        }
                    });
                    page++;
                    hasNextPage = publicationBatch.size() == batchSize;
                    break;
                default:
                    hasNextPage = false;
                    break;
            }
        }
    }

    public <T extends DocumentDTO> void saveWithDuplicateCheck(
        T dto, String sourceId,
        boolean performIndex,
        BiConsumer<T, Boolean> saveFn,
        String entityLabel,
        DocumentPublicationService documentPublicationService) {

        if (Objects.isNull(dto)) {
            return;
        }

        try {
            saveFn.accept(dto, performIndex);
        } catch (Exception e) {
            log.warn("Skipped loading object of type '{}' with id '{}'. Reason: '{}'.",
                entityLabel, sourceId, e.getMessage());

            documentPublicationService.findDocumentByCommonIdentifier(
                    dto.getDoi(), dto.getOpenAlexId(),
                    dto.getScopusId(), dto.getWebOfScienceId())
                .ifPresent(existingDuplicate -> dto.getTitle().forEach(title -> {
                    var content = title.getContent().trim();
                    boolean isTrueMatch = existingDuplicate.getTitle().stream()
                        .anyMatch(mc -> mc.getContent().trim().equalsIgnoreCase(content));

                    if (!isTrueMatch) {
                        try {
                            dto.setDoi(null);
                            dto.setOpenAlexId(null);
                            dto.setScopusId(null);
                            dto.setWebOfScienceId(null);
                            saveFn.accept(dto, performIndex);

                            log.info("Added {} '{}' without identifiers ({}, {}, {}, {}).",
                                entityLabel, sourceId,
                                existingDuplicate.getDoi(),
                                existingDuplicate.getOpenAlexId(),
                                existingDuplicate.getScopusId(),
                                existingDuplicate.getWebOfScienceId());
                        } catch (Exception ex) {
                            log.info(
                                "Tried to add {} '{}' after removing identifiers. Failed because: {}",
                                entityLabel, sourceId, ex.getMessage());
                        }
                    } else {
                        var identifierUpdateRequest = new DocumentIdentifierUpdateDTO();

                        if (!StringUtil.valueExists(existingDuplicate.getDoi()) &&
                            StringUtil.valueExists(dto.getDoi())) {
                            identifierUpdateRequest.setDoi(dto.getDoi());
                        }

                        if (!StringUtil.valueExists(existingDuplicate.getScopusId()) &&
                            StringUtil.valueExists(dto.getScopusId())) {
                            identifierUpdateRequest.setScopusId(dto.getScopusId());
                        }

                        if (!StringUtil.valueExists(existingDuplicate.getOpenAlexId()) &&
                            StringUtil.valueExists(dto.getOpenAlexId())) {
                            identifierUpdateRequest.setOpenAlexId(dto.getOpenAlexId());
                        }

                        if (!StringUtil.valueExists(existingDuplicate.getWebOfScienceId()) &&
                            StringUtil.valueExists(dto.getWebOfScienceId())) {
                            identifierUpdateRequest.setWebOfScienceId(dto.getWebOfScienceId());
                        }

                        documentPublicationService.updateDocumentIdentifiers(
                            existingDuplicate.getId(), identifierUpdateRequest);

                        try {
                            log.info("Updated identifiers for {} '{}' ({}, {}, {}, {}).",
                                entityLabel, sourceId,
                                existingDuplicate.getDoi(),
                                existingDuplicate.getOpenAlexId(),
                                existingDuplicate.getScopusId(),
                                existingDuplicate.getWebOfScienceId());
                        } catch (Exception ex) {
                            log.info(
                                "Tried to enrich identifiers for {} '{}'. Failed because: {}",
                                entityLabel, sourceId, ex.getMessage());
                        }
                    }
                }));
        }
    }
}
