package rs.teslaris.core.importer.service.impl;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.importer.model.common.DocumentImport;
import rs.teslaris.core.importer.model.common.OrganisationUnit;
import rs.teslaris.core.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.core.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.core.importer.service.interfaces.CommonLoader;
import rs.teslaris.core.importer.utility.DataSet;
import rs.teslaris.core.importer.utility.ProgressReportUtility;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.JournalService;
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

    private final LanguageTagService languageTagService;


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
        documentImport.getPublishedIn().forEach(name -> {
            var languageTag =
                languageTagService.findLanguageTagByValue(name.getLanguageTag());
            journalDTO.getTitle().add(
                new MultilingualContentDTO(languageTag.getId(), name.getLanguageTag(),
                    name.getContent(), name.getPriority()));
        });
        journalDTO.setEissn(documentImport.getEIssn());
        journalDTO.setPrintISSN(documentImport.getPrintIssn());

        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        journalService.createJournal(journalDTO, true);
        return journalDTO;
    }

    private OrganisationUnitDTO createLoadedInstitution(OrganisationUnit institution) {
        var creationDTO = new OrganisationUnitRequestDTO();
        creationDTO.setName(new ArrayList<>());
        institution.getName().forEach(name -> {
            var languageTag =
                languageTagService.findLanguageTagByValue(name.getLanguageTag());
            creationDTO.getName().add(
                new MultilingualContentDTO(languageTag.getId(), name.getLanguageTag(),
                    name.getContent(), name.getPriority()));
        });
        creationDTO.setNameAbbreviation(
            Objects.nonNull(institution.getNameAbbreviation()) ?
                institution.getNameAbbreviation() : "");
        creationDTO.setScopusAfid(institution.getScopusAfid());
        creationDTO.setKeyword(new ArrayList<>());
        creationDTO.setResearchAreasId(new ArrayList<>());
        creationDTO.setContact(new ContactDTO());
        creationDTO.setLocation(new GeoLocationDTO());

        return organisationUnitService.createOrganisationUnit(creationDTO, true);
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
