package rs.teslaris.core.importer.service.impl;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.importer.model.common.DocumentImport;
import rs.teslaris.core.importer.model.common.Event;
import rs.teslaris.core.importer.model.common.MultilingualContent;
import rs.teslaris.core.importer.model.common.OrganisationUnit;
import rs.teslaris.core.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.core.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.core.importer.service.interfaces.CommonLoader;
import rs.teslaris.core.importer.utility.DataSet;
import rs.teslaris.core.importer.utility.ProgressReportUtility;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.RecordAlreadyLoadedException;

@Service
@RequiredArgsConstructor
public class CommonLoaderImpl implements CommonLoader {

    private final MongoTemplate mongoTemplate;

    private final JournalPublicationConverter journalPublicationConverter;

    private final ProceedingsPublicationConverter proceedingsPublicationConverter;

    private final OrganisationUnitService organisationUnitService;

    private final JournalService journalService;

    private final ConferenceService conferenceService;

    private final ProceedingsService proceedingsService;

    private final LanguageTagService languageTagService;

    private final PublicationSeriesService publicationSeriesService;


    @Override
    public <R> R loadRecordsWizard(Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("import_users_id").in(userId));
        query.addCriteria(Criteria.where("is_loaded").is(false));

        var progressReport =
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                mongoTemplate);
        if (progressReport != null) {
            query.addCriteria(Criteria.where("identifier").gte(progressReport.getLastLoadedId()));
        } else {
            query.addCriteria(Criteria.where("identifier").gte(""));
        }
        query.limit(1);

        return findAndConvertEntity(query, userId);
    }

    @Override
    public <R> R loadSkippedRecordsWizard(Integer userId) {
        ProgressReportUtility.resetProgressReport(DataSet.DOCUMENT_IMPORTS, userId, mongoTemplate);
        return loadRecordsWizard(userId);
    }

    @Override
    public void skipRecord(Integer userId) {
        var progressReport =
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                mongoTemplate);
        Query nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(
            Criteria.where("identifier").gt(progressReport.getLastLoadedId()));

        var nextRecord = mongoTemplate.findOne(nextRecordQuery, DocumentImport.class);
        if (Objects.nonNull(nextRecord)) {
            Method getIdMethod;
            try {
                getIdMethod = DocumentImport.class.getMethod("getIdentifier");
                progressReport.setLastLoadedId((String) getIdMethod.invoke(nextRecord));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                return;
            }
        } else {
            progressReport.setLastLoadedId("");
        }

        ProgressReportUtility.deleteProgressReport(DataSet.DOCUMENT_IMPORTS, userId, mongoTemplate);
        mongoTemplate.save(progressReport);
    }

    @Override
    public void markRecordAsLoaded(Integer userId) {
        var progressReport =
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                mongoTemplate);

        Query query = new Query();
        query.addCriteria(Criteria.where("identifier").is(progressReport.getLastLoadedId()));
        query.addCriteria(Criteria.where("import_users_id").is(userId));
        query.addCriteria(Criteria.where("loaded").is(false));

        var entityClass = DataSet.getClassForValue(DataSet.DOCUMENT_IMPORTS.getStringValue());

        var updateOperation = new Update();
        updateOperation.set("loaded", true);

        var updatedRecord = mongoTemplate.findAndModify(query, updateOperation,
            new FindAndModifyOptions().returnNew(true).upsert(false),
            entityClass);

        if (Objects.isNull(updatedRecord)) {
            throw new RecordAlreadyLoadedException("recordAlreadyLoadedMessage");
        }
    }

    @Override
    public Integer countRemainingDocumentsForLoading(Integer userId) {
        var countQuery = new Query();
        countQuery.addCriteria(Criteria.where("loaded").is(false));
        countQuery.addCriteria(Criteria.where("import_users_id").in(userId));

        return Math.toIntExact(mongoTemplate.count(countQuery, DocumentImport.class));
    }

    @Override
    public OrganisationUnitDTO createInstitution(String scopusAfid, Integer userId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        for (var contribution : currentlyLoadedEntity.getContributions()) {
            for (var institution : contribution.getInstitutions()) {
                if (institution.getScopusAfid().equals(scopusAfid) && Objects.isNull(
                    organisationUnitService.findOrganisationUnitByScopusAfid(scopusAfid))) {

                    return createLoadedInstitution(institution);
                }
            }
        }

        throw new NotFoundException("Institution with given AFID is not loaded.");
    }

    @Override
    public PublicationSeriesDTO createJournal(String eIssn, String printIssn, Integer userId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        if ((Objects.nonNull(currentlyLoadedEntity.getEIssn()) &&
            currentlyLoadedEntity.getEIssn().equals(eIssn)) ||
            (Objects.nonNull(currentlyLoadedEntity.getPrintIssn()) &&
                currentlyLoadedEntity.getPrintIssn().equals(printIssn))) {
            return createJournal(currentlyLoadedEntity);
        }

        throw new NotFoundException("Journal with given ISSN is not loaded.");
    }

    @Override
    public ProceedingsDTO createProceedings(Integer userId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        var createdConference = createConference(currentlyLoadedEntity.getEvent());
        return createProceedings(currentlyLoadedEntity, createdConference.getId());
    }

    private DocumentImport retrieveCurrentlyLoadedEntity(Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("import_users_id").in(userId));
        query.addCriteria(Criteria.where("is_loaded").is(false));

        var progressReport =
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                mongoTemplate);
        if (progressReport != null) {
            query.addCriteria(Criteria.where("identifier").gte(progressReport.getLastLoadedId()));
        } else {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    private PublicationSeriesDTO createJournal(DocumentImport documentImport) {
        var journalDTO = new PublicationSeriesDTO();

        journalDTO.setTitle(new ArrayList<>());
        setMultilingualContent(journalDTO.getTitle(), documentImport.getPublishedIn());

        journalDTO.setEissn(documentImport.getEIssn());
        journalDTO.setPrintISSN(documentImport.getPrintIssn());

        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        var createdJournal = journalService.createJournal(journalDTO, true);
        journalDTO.setId(createdJournal.getId());

        return journalDTO;
    }

    private ProceedingsDTO createProceedings(DocumentImport proceedingsPublication,
                                             Integer eventId) {
        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setEventId(eventId);

        proceedingsDTO.setTitle(new ArrayList<>());
        setMultilingualContent(proceedingsDTO.getTitle(), proceedingsPublication.getPublishedIn());

        proceedingsDTO.setSubTitle(new ArrayList<>());
        proceedingsDTO.setDescription(new ArrayList<>());
        proceedingsDTO.setKeywords(new ArrayList<>());
        proceedingsDTO.setContributions(new ArrayList<>());
        proceedingsDTO.setUris(new HashSet<>());
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());

        var publicationSeries =
            publicationSeriesService.findPublicationSeriesByIssn(proceedingsPublication.getEIssn(),
                proceedingsPublication.getPrintIssn());

        if (Objects.nonNull(publicationSeries)) {
            proceedingsDTO.setPublicationSeriesId(publicationSeries.getId());
        }

        var createdProceedings = proceedingsService.createProceedings(proceedingsDTO, true);
        proceedingsDTO.setId(createdProceedings.getId());
        return proceedingsDTO;
    }

    private Conference createConference(Event conference) {
        var conferenceDTO = new ConferenceDTO();

        conferenceDTO.setName(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getName(), conference.getName());

        conferenceDTO.setNameAbbreviation(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getNameAbbreviation(),
            conference.getNameAbbreviation());

        conferenceDTO.setDescription(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getDescription(), conference.getDescription());

        conferenceDTO.setKeywords(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getKeywords(), conference.getKeywords());

        conferenceDTO.setState(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getState(), conference.getState());

        conferenceDTO.setPlace(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getPlace(), conference.getPlace());

        conferenceDTO.setSerialEvent(conference.getSerialEvent());
        conferenceDTO.setDateFrom(conference.getDateFrom());
        conferenceDTO.setDateTo(conference.getDateTo());

        return conferenceService.createConference(conferenceDTO, true);
    }

    private void setMultilingualContent(List<MultilingualContentDTO> targetList,
                                        List<MultilingualContent> sourceList) {
        sourceList.forEach(sourceItem -> {
            if (Objects.isNull(sourceItem.getContent())) {
                return;
            }

            var languageTag =
                languageTagService.findLanguageTagByValue(sourceItem.getLanguageTag());
            targetList.add(
                new MultilingualContentDTO(languageTag.getId(), sourceItem.getLanguageTag(),
                    sourceItem.getContent(), sourceItem.getPriority()));
        });
    }

    private OrganisationUnitDTO createLoadedInstitution(OrganisationUnit institution) {
        var organisationUnitDTO = new OrganisationUnitRequestDTO();

        organisationUnitDTO.setName(new ArrayList<>());
        setMultilingualContent(organisationUnitDTO.getName(), institution.getName());

        organisationUnitDTO.setNameAbbreviation(
            Objects.nonNull(institution.getNameAbbreviation()) ?
                institution.getNameAbbreviation() : "");
        organisationUnitDTO.setScopusAfid(institution.getScopusAfid());
        organisationUnitDTO.setKeyword(new ArrayList<>());
        organisationUnitDTO.setResearchAreasId(new ArrayList<>());
        organisationUnitDTO.setContact(new ContactDTO());
        organisationUnitDTO.setLocation(new GeoLocationDTO());

        return organisationUnitService.createOrganisationUnit(organisationUnitDTO, true);
    }

    @Nullable
    private <R> R findAndConvertEntity(Query query, Integer userId) {
        var entity = mongoTemplate.findOne(query, DocumentImport.class, "documentImports");

        if (Objects.nonNull(entity)) {
            Method getIdMethod;

            try {
                getIdMethod = DocumentImport.class.getMethod("getIdentifier");
            } catch (NoSuchMethodException e) {
                return null;
            }

            try {
                ProgressReportUtility.updateProgressReport(DataSet.DOCUMENT_IMPORTS,
                    (String) getIdMethod.invoke(entity), userId, mongoTemplate);
            } catch (IllegalAccessException | InvocationTargetException e) {
                return null;
            }

            switch (entity.getPublicationType()) {
                case JOURNAL_PUBLICATION -> {
                    return (R) journalPublicationConverter.toImportDTO(entity);
                }
                case PROCEEDINGS_PUBLICATION -> {
                    return (R) proceedingsPublicationConverter.toImportDTO(entity);
                }
            }

        }
        return null;
    }
}
