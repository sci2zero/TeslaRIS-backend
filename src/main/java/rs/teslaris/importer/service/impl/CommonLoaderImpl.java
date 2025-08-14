package rs.teslaris.importer.service.impl;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.person.PersonConverter;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.RecordAlreadyLoadedException;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.importer.service.interfaces.CommonLoader;
import rs.teslaris.importer.service.interfaces.LoadingConfigurationService;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.LoadProgressReport;
import rs.teslaris.importer.utility.ProgressReportUtility;

@Service
@RequiredArgsConstructor
@Traceable
@Transactional
public class CommonLoaderImpl implements CommonLoader {

    private static final Object institutionLock = new Object();

    private static final Object personLock = new Object();

    private static final Object journalLock = new Object();

    private static final Object eventLock = new Object();

    private final MongoTemplate mongoTemplate;

    private final JournalPublicationConverter journalPublicationConverter;

    private final ProceedingsPublicationConverter proceedingsPublicationConverter;

    private final OrganisationUnitService organisationUnitService;

    private final JournalService journalService;

    private final ConferenceService conferenceService;

    private final ProceedingsService proceedingsService;

    private final LanguageTagService languageTagService;

    private final PublicationSeriesService publicationSeriesService;

    private final CountryService countryService;

    private final PersonService personService;

    private final LoadingConfigurationService loadingConfigurationService;

    private final DocumentPublicationService documentPublicationService;


    @Override
    public <R> R loadRecordsWizard(Integer userId, Integer institutionId) {
        Query query = new Query();
        if (Objects.nonNull(institutionId)) {
            query.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        } else {
            query.addCriteria(Criteria.where("import_users_id").in(userId));
        }
        query.addCriteria(Criteria.where("is_loaded").is(false));

        var progressReport =
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, institutionId,
                mongoTemplate);
        if (Objects.nonNull(progressReport)) {
            query.addCriteria(
                Criteria.where("_id").gte(progressReport.getLastLoadedId()));
        } else {
            query.addCriteria(Criteria.where("identifier").gte(""));
        }
        query.limit(1);

        return findAndConvertEntity(query, userId, institutionId);
    }

    @Override
    public <R> R loadSkippedRecordsWizard(Integer userId, Integer institutionId) {
        ProgressReportUtility.resetProgressReport(DataSet.DOCUMENT_IMPORTS, userId, institutionId,
            mongoTemplate);
        return loadRecordsWizard(userId, institutionId);
    }

    @Override
    public void skipRecord(Integer userId, Integer institutionId, Boolean removeFromRecord) {
        var progressReport =
            Objects.requireNonNullElse(
                ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                    institutionId, mongoTemplate),
                new LoadProgressReport("1", new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID),
                    userId,
                    institutionId,
                    DataSet.DOCUMENT_IMPORTS));

        if (removeFromRecord &&
            Objects.requireNonNull(SessionTrackingUtil.getLoggedInUser()).getAuthority().getName()
                .equals(UserRole.RESEARCHER.name())) {
            Query currentRecordQuery = new Query();
            currentRecordQuery.addCriteria(
                Criteria.where("_id")
                    .is(new ObjectId(progressReport.getLastLoadedId().toHexString())));

            var currentRecord = mongoTemplate.findOne(currentRecordQuery, DocumentImport.class);
            if (Objects.nonNull(currentRecord)) {
                currentRecord.getImportUsersId().remove(userId);

                mongoTemplate.save(currentRecord);
            }
        }

        Query nextRecordQuery = new Query();
        if (Objects.nonNull(institutionId)) {
            nextRecordQuery.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        } else {
            nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        }
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(
            Criteria.where("_id").gt(new ObjectId(progressReport.getLastLoadedId().toHexString())));

        var nextRecord = mongoTemplate.findOne(nextRecordQuery, DocumentImport.class);
        if (Objects.nonNull(nextRecord)) {
            Method getIdMethod, getIdentifierMethod;
            try {
                getIdentifierMethod = DocumentImport.class.getMethod("getIdentifier");
                getIdMethod = DocumentImport.class.getMethod("getId");
                progressReport.setLastLoadedIdentifier(
                    (String) getIdentifierMethod.invoke(nextRecord));
                progressReport.setLastLoadedId(
                    new ObjectId((String) getIdMethod.invoke(nextRecord)));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                return;
            }
        } else {
            progressReport.setLastLoadedIdentifier("");
            progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        }

        ProgressReportUtility.deleteProgressReport(
            DataSet.DOCUMENT_IMPORTS, userId, institutionId, mongoTemplate);
        mongoTemplate.save(progressReport);
    }

    @Override
    public void markRecordAsLoaded(Integer userId, Integer institutionId, Integer oldDocumentId,
                                   Boolean deleteOldDocument) {
        var progressReport =
            Objects.requireNonNullElse(
                ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                    institutionId,
                    mongoTemplate),
                new LoadProgressReport("1", new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID),
                    userId,
                    institutionId,
                    DataSet.DOCUMENT_IMPORTS));

        Query query = new Query();
        query.addCriteria(
            Criteria.where("identifier").is(progressReport.getLastLoadedIdentifier()));
        if (Objects.nonNull(institutionId)) {
            query.addCriteria(Criteria.where("import_institutions_id").is(institutionId));
        } else {
            query.addCriteria(Criteria.where("import_users_id").is(userId));
        }
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

        updatePersonInvolvements((DocumentImport) updatedRecord);

        handleDeduplication(oldDocumentId, deleteOldDocument, (DocumentImport) updatedRecord,
            progressReport);
    }

    @Override
    public Integer countRemainingDocumentsForLoading(Integer userId, Integer institutionId) {
        var countQuery = new Query();
        countQuery.addCriteria(Criteria.where("loaded").is(false));
        if (Objects.nonNull(institutionId)) {
            countQuery.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        } else {
            countQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        }

        return Math.toIntExact(mongoTemplate.count(countQuery, DocumentImport.class));
    }

    @Override
    public OrganisationUnitDTO createInstitution(String importId, Integer userId,
                                                 Integer institutionId) {
        var isUnmanagedLoading = isLoadedAsUnmanagedEntity(userId, institutionId);
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        for (var contribution : currentlyLoadedEntity.getContributions()) {
            for (var institution : contribution.getInstitutions()) {
                if (institution.getImportId().equals(importId) && Objects.isNull(
                    organisationUnitService.findOrganisationUnitByImportId(importId))) {

                    if (isUnmanagedLoading) {
                        return new OrganisationUnitDTO() {{
                            setName(new ArrayList<>());
                            setMultilingualContent(getName(), institution.getName());
                        }};
                    }

                    return createLoadedInstitution(institution);
                }
            }
        }

        throw new NotFoundException("Institution with given import ID is not loaded.");
    }

    @Override
    public PersonResponseDTO createPerson(String importId, Integer userId,
                                          Integer institutionId) {
        var isUnmanagedLoading = isLoadedAsUnmanagedEntity(userId, institutionId);
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        for (var contribution : currentlyLoadedEntity.getContributions()) {
            if (contribution.getPerson().getImportId().equals(importId) &&
                Objects.isNull(personService.findPersonByImportIdentifier(importId))) {

                if (isUnmanagedLoading) {
                    return new PersonResponseDTO() {{
                        setPersonName(new PersonNameDTO(null,
                            contribution.getPerson().getName().getFirstName(),
                            contribution.getPerson().getName().getMiddleName(),
                            contribution.getPerson().getName().getLastName(), null, null));
                    }};
                }

                var savedPerson = createLoadedPerson(contribution.getPerson());

                var pastContributionInstitutionIdentifiers = new HashSet<String>();
                contribution.getInstitutions().forEach(institution -> {
                    if (pastContributionInstitutionIdentifiers.contains(
                        institution.getImportId())) {
                        return;
                    }

                    var institutionIndex =
                        organisationUnitService.findOrganisationUnitByImportId(
                            institution.getImportId());

                    if (Objects.nonNull(institutionIndex)) {
                        var employmentInstitution =
                            organisationUnitService.findOne(institutionIndex.getDatabaseId());
                        var currentEmployment =
                            new Employment(null, null, ApproveStatus.APPROVED, new HashSet<>(),
                                InvolvementType.EMPLOYED_AT, new HashSet<>(), null,
                                employmentInstitution, null, new HashSet<>());
                        savedPerson.addInvolvement(currentEmployment);

                        pastContributionInstitutionIdentifiers.add(institution.getImportId());
                    }
                });

                personService.save(savedPerson);
                personService.indexPerson(savedPerson);

                return PersonConverter.toDTO(savedPerson);
            }
        }

        throw new NotFoundException("Person with given import ID is not loaded.");
    }

    public void updatePersonInvolvements(DocumentImport currentlyLoadedEntity) {
        for (var contribution : currentlyLoadedEntity.getContributions()) {
            var scopusAuthorId = contribution.getPerson().getScopusAuthorId();

            var savedPersonIndex = personService.findPersonByImportIdentifier(scopusAuthorId);
            if (Objects.isNull(savedPersonIndex)) {
                continue; // does not exist because person is added as non-managed entity
            }

            var savedPerson = personService.findOne(savedPersonIndex.getDatabaseId());

            contribution.getInstitutions().forEach(institution -> {
                var institutionIndex =
                    organisationUnitService.findOrganisationUnitByImportId(
                        institution.getImportId());

                if (Objects.isNull(institutionIndex) ||
                    savedPerson.getInvolvements().stream().anyMatch(
                        i -> (i.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                            i.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                            Objects.nonNull(i.getOrganisationUnit()) &&
                            i.getOrganisationUnit().getId()
                                .equals(institutionIndex.getDatabaseId()))) {
                    return;
                }

                var employmentInstitution =
                    organisationUnitService.findOne(institutionIndex.getDatabaseId());
                var currentEmployment =
                    new Employment(null, null, ApproveStatus.APPROVED, new HashSet<>(),
                        InvolvementType.EMPLOYED_AT, new HashSet<>(), null,
                        employmentInstitution, null, new HashSet<>());
                savedPerson.addInvolvement(currentEmployment);
            });

            personService.save(savedPerson);
            personService.indexPerson(savedPerson);
        }
    }

    private void handleDeduplication(Integer oldDocumentId, Boolean deleteOldDocument,
                                     DocumentImport updatedRecord,
                                     LoadProgressReport progressReport) {
        if (Objects.nonNull(oldDocumentId) && oldDocumentId > 0 &&
            Objects.nonNull(deleteOldDocument)) {
            var potentialDuplicateIds = fetchPotentialDuplicateIds(updatedRecord);

            if (!potentialDuplicateIds.contains(oldDocumentId)) {
                return;
            }

            if (deleteOldDocument) {
                documentPublicationService.deleteDocumentPublication(oldDocumentId);
            } else {
                var oldDocument = documentPublicationService.findDocumentById(oldDocumentId);
                oldDocument.setScopusId(progressReport.getLastLoadedIdentifier());
                documentPublicationService.save(oldDocument);
            }
        }
    }

    private List<Integer> fetchPotentialDuplicateIds(DocumentImport record) {
        var doi = Objects.requireNonNullElse(record.getDoi(), "");
        var scopus =
            Objects.requireNonNullElse(record.getScopusId(), "");
        var openAlex =
            Objects.requireNonNullElse(record.getOpenAlexId(), "");
        var wos =
            Objects.requireNonNullElse(record.getWebOfScienceId(), "");
        var titles = record.getTitle().stream().map(
            MultilingualContent::getContent).toList();

        return documentPublicationService.findDocumentDuplicates(titles, doi, scopus, openAlex, wos)
            .getContent()
            .stream().map(
                DocumentPublicationIndex::getDatabaseId).toList();
    }

    @Override
    public void prepareOldDocumentForOverwriting(Integer userId, Integer institutionId,
                                                 Integer oldDocumentId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        var potentialDuplicateIds = fetchPotentialDuplicateIds(currentlyLoadedEntity);
        if (potentialDuplicateIds.contains(oldDocumentId)) {
            var document = documentPublicationService.findOne(oldDocumentId);
            document.setDoi("");
            document.setScopusId("");
            document.setOpenAlexId("");
            document.setWebOfScienceId("");
            documentPublicationService.save(document);
        }
    }

    @Override
    public PublicationSeriesDTO createJournal(String eIssn, String printIssn, Integer userId,
                                              Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        if ((Objects.nonNull(currentlyLoadedEntity.getEIssn()) &&
            currentlyLoadedEntity.getEIssn().equals(eIssn)) ||
            (Objects.nonNull(currentlyLoadedEntity.getPrintIssn()) &&
                currentlyLoadedEntity.getPrintIssn().equals(printIssn)) ||
            (Objects.isNull(currentlyLoadedEntity.getEIssn()) &&
                Objects.isNull(currentlyLoadedEntity.getPrintIssn()) &&
                eIssn.equals("NONE") && printIssn.equals("NONE"))) {
            return createJournal(currentlyLoadedEntity);
        }

        throw new NotFoundException("Journal with given ISSN is not loaded.");
    }

    @Override
    public ProceedingsDTO createProceedings(Integer userId, Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        var createdConference = createConference(currentlyLoadedEntity.getEvent());
        return createProceedings(currentlyLoadedEntity, createdConference.getId());
    }

    @Override
    public void updateManuallySelectedPersonIdentifiers(String importId, Integer selectedPersonId,
                                                        Integer userId, Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        for (var contribution : currentlyLoadedEntity.getContributions()) {
            var person = contribution.getPerson();
            if (importId.equals(person.getImportId())) {
                var savedPerson = personService.findOne(selectedPersonId);

                copyMissingPersonIdentifiers(person, savedPerson);
                personService.save(savedPerson);
                personService.indexPerson(savedPerson);
                return;
            }
        }

        throw new NotFoundException(
            "Person with import ID " + importId + " not found among contributions.");
    }

    @Override
    public void updateManuallySelectedInstitutionIdentifiers(String importId,
                                                             Integer selectedInstitutionId,
                                                             Integer userId,
                                                             Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        for (var contribution : currentlyLoadedEntity.getContributions()) {
            for (var institution : contribution.getInstitutions()) {
                if (institution.getImportId().equals(importId)) {
                    var savedInstitution = organisationUnitService.findOne(selectedInstitutionId);

                    copyMissingInstitutionIdentifiers(institution, savedInstitution);
                    organisationUnitService.save(savedInstitution);
                    organisationUnitService.indexOrganisationUnit(savedInstitution,
                        savedInstitution.getId());

                    return;
                }
            }
        }

        throw new NotFoundException(
            "Institution with import ID " + importId +
                " not found among contribution affiliations.");
    }

    @Override
    public void updateManuallySelectedPublicationSeriesIdentifiers(String eIssn, String printIssn,
                                                                   Integer selectedPubSeriesId,
                                                                   Integer userId,
                                                                   Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        if ((Objects.nonNull(currentlyLoadedEntity.getEIssn()) &&
            currentlyLoadedEntity.getEIssn().equals(eIssn)) ||
            (Objects.nonNull(currentlyLoadedEntity.getPrintIssn()) &&
                currentlyLoadedEntity.getPrintIssn().equals(printIssn)) ||
            (Objects.isNull(currentlyLoadedEntity.getEIssn()) &&
                Objects.isNull(currentlyLoadedEntity.getPrintIssn()) &&
                eIssn.equals("NONE") && printIssn.equals("NONE"))) {

            var journal = journalService.findJournalById(selectedPubSeriesId);

            copyMissingPubSeriesIdentifiers(currentlyLoadedEntity, journal);
            publicationSeriesService.save(journal);
            journalService.indexJournal(journal);

            return;
        }

        throw new NotFoundException("Journal with given ISSN is not loaded.");
    }

    @Override
    public void updateManuallySelectedConferenceIdentifiers(Integer selectedConferenceId,
                                                            Integer userId, Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        var conference = conferenceService.findConferenceById(selectedConferenceId);

        if (Objects.nonNull(currentlyLoadedEntity.getEvent())) {
            copyMissingEventIdentifiers(currentlyLoadedEntity.getEvent(), conference);
            conferenceService.save(conference);
            conferenceService.indexConference(conference);
        }
    }

    private void copyMissingPersonIdentifiers(Person from,
                                              rs.teslaris.core.model.person.Person to) {
        if (Objects.nonNull(from.getOpenAlexId()) &&
            (Objects.isNull(to.getOpenAlexId()) || to.getOpenAlexId().isBlank())) {
            to.setOpenAlexId(from.getOpenAlexId());
        }

        if (Objects.nonNull(from.getOrcid()) &&
            (Objects.isNull(to.getOrcid()) || to.getOrcid().isBlank())) {
            to.setOrcid(from.getOrcid());
        }

        if (Objects.nonNull(from.getScopusAuthorId()) &&
            (Objects.isNull(to.getScopusAuthorId()) || to.getScopusAuthorId().isBlank())) {
            to.setScopusAuthorId(from.getScopusAuthorId());
        }

        if (Objects.nonNull(from.getWebOfScienceResearcherId()) &&
            (Objects.isNull(to.getWebOfScienceResearcherId()) ||
                to.getWebOfScienceResearcherId().isBlank())) {
            to.setWebOfScienceResearcherId(from.getWebOfScienceResearcherId());
        }
    }

    private void copyMissingInstitutionIdentifiers(OrganisationUnit from,
                                                   rs.teslaris.core.model.institution.OrganisationUnit to) {
        if (Objects.nonNull(from.getOpenAlexId()) &&
            (Objects.isNull(to.getOpenAlexId()) || to.getOpenAlexId().isBlank())) {
            to.setOpenAlexId(from.getOpenAlexId());
        }

        if (Objects.nonNull(from.getScopusAfid()) &&
            (Objects.isNull(to.getScopusAfid()) || to.getScopusAfid().isBlank())) {
            to.setScopusAfid(from.getScopusAfid());
        }

        if (Objects.nonNull(from.getRor()) &&
            (Objects.isNull(to.getRor()) || to.getRor().isBlank())) {
            to.setRor(from.getRor());
        }
    }

    private void copyMissingPubSeriesIdentifiers(DocumentImport from,
                                                 PublicationSeries to) {
        if (Objects.nonNull(from.getJournalOpenAlexId()) &&
            (Objects.isNull(to.getOpenAlexId()) || to.getOpenAlexId().isBlank())) {
            to.setOpenAlexId(from.getJournalOpenAlexId());
        }

        if (Objects.nonNull(from.getEIssn()) &&
            (Objects.isNull(to.getEISSN()) || to.getEISSN().isBlank())) {
            to.setEISSN(from.getEIssn());
        }

        if (Objects.nonNull(from.getPrintIssn()) &&
            (Objects.isNull(to.getPrintISSN()) || to.getPrintISSN().isBlank())) {
            to.setPrintISSN(from.getPrintIssn());
        }
    }

    private void copyMissingEventIdentifiers(Event from,
                                             Conference to) {
        if (Objects.nonNull(from.getOpenAlexId()) &&
            (Objects.isNull(to.getOpenAlexId()) || to.getOpenAlexId().isBlank())) {
            to.setOpenAlexId(from.getOpenAlexId());
        }

        if (Objects.nonNull(from.getConfId()) &&
            (Objects.isNull(to.getConfId()) || to.getConfId().isBlank())) {
            to.setConfId(from.getConfId());
        }
    }

    private DocumentImport retrieveCurrentlyLoadedEntity(Integer userId, Integer institutionId) {
        Query query = new Query();
        if (Objects.nonNull(institutionId)) {
            query.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        } else {
            query.addCriteria(Criteria.where("import_users_id").in(userId));
        }
        query.addCriteria(Criteria.where("is_loaded").is(false));

        var progressReport =
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, institutionId,
                mongoTemplate);
        if (progressReport != null) {
            query.addCriteria(
                Criteria.where("identifier").is(progressReport.getLastLoadedIdentifier()));
        } else {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    private PublicationSeriesDTO createJournal(DocumentImport documentImport) {
        // Lock hint
        var potentialMatch = searchPotentialMatches(documentImport);
        if (Objects.nonNull(potentialMatch)) {
            return potentialMatch;
        }

        var journalDTO = new PublicationSeriesDTO();

        journalDTO.setTitle(new ArrayList<>());
        setMultilingualContent(journalDTO.getTitle(), documentImport.getPublishedIn());

        journalDTO.setEissn(documentImport.getEIssn());
        journalDTO.setPrintISSN(documentImport.getPrintIssn());
        journalDTO.setOpenAlexId(documentImport.getJournalOpenAlexId());

        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        synchronized (journalLock) {
            potentialMatch = searchPotentialMatches(documentImport);
            if (Objects.nonNull(potentialMatch)) {
                return potentialMatch;
            }

            var createdJournal = journalService.createJournal(journalDTO, true);
            journalDTO.setId(createdJournal.getId());

            return journalDTO;
        }
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
        // Lock hint
        var potentialMatch = searchPotentialMatches(conference);
        if (Objects.nonNull(potentialMatch)) {
            return potentialMatch;
        }

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

        conferenceDTO.setPlace(new ArrayList<>());
        setMultilingualContent(conferenceDTO.getPlace(), conference.getPlace());

        for (var stateMC : conference.getState()) {
            var country = countryService.findCountryByName(stateMC.getContent());
            if (country.isPresent()) {
                conferenceDTO.setCountryId(country.get().getId());
                break;
            }
        }

        conferenceDTO.setSerialEvent(conference.getSerialEvent());
        conferenceDTO.setDateFrom(conference.getDateFrom());
        conferenceDTO.setDateTo(conference.getDateTo());
        conferenceDTO.setOpenAlexId(conference.getOpenAlexId());

        synchronized (eventLock) {
            potentialMatch = searchPotentialMatches(conference);
            if (Objects.nonNull(potentialMatch)) {
                return potentialMatch;
            }

            return conferenceService.createConference(conferenceDTO, true);
        }
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
        // Lock hint
        var potentialMatch = searchPotentialMatches(institution);
        if (Objects.nonNull(potentialMatch)) {
            return potentialMatch;
        }

        var organisationUnitDTO = new OrganisationUnitRequestDTO();

        organisationUnitDTO.setName(new ArrayList<>());
        setMultilingualContent(organisationUnitDTO.getName(), institution.getName());

        organisationUnitDTO.setNameAbbreviation(
            Objects.nonNull(institution.getNameAbbreviation()) ?
                institution.getNameAbbreviation() : "");
        organisationUnitDTO.setScopusAfid(institution.getScopusAfid());
        organisationUnitDTO.setOpenAlexId(institution.getOpenAlexId());
        organisationUnitDTO.setKeyword(new ArrayList<>());
        organisationUnitDTO.setResearchAreasId(new ArrayList<>());
        organisationUnitDTO.setContact(new ContactDTO());
        organisationUnitDTO.setLocation(new GeoLocationDTO());

        synchronized (institutionLock) {
            potentialMatch = searchPotentialMatches(institution);
            if (Objects.nonNull(potentialMatch)) {
                return potentialMatch;
            }

            return organisationUnitService.createOrganisationUnit(organisationUnitDTO, true);
        }
    }

    private rs.teslaris.core.model.person.Person createLoadedPerson(Person person) {
        // Lock hint
        var potentialMatch = searchPotentialMatches(person);
        if (Objects.nonNull(potentialMatch)) {
            return potentialMatch;
        }

        var basicPersonDTO = getBasicPersonDTO(person);

        synchronized (personLock) {
            potentialMatch = searchPotentialMatches(person);
            if (Objects.nonNull(potentialMatch)) {
                return potentialMatch;
            }

            return personService.importPersonWithBasicInfo(basicPersonDTO, true);
        }
    }

    @Nullable
    private OrganisationUnitDTO searchPotentialMatches(OrganisationUnit institution) {
        var potentialMatch =
            organisationUnitService.findOrganisationUnitByImportId(institution.getImportId());
        if (Objects.nonNull(potentialMatch)) {
            var existingRecordResponse = new OrganisationUnitDTO();
            existingRecordResponse.setName(List.of(new MultilingualContentDTO(-1, "",
                potentialMatch.getNameSr(), 1)));
            existingRecordResponse.setId(potentialMatch.getDatabaseId());
            return existingRecordResponse;
        }

        return null;
    }

    @Nullable
    private rs.teslaris.core.model.person.Person searchPotentialMatches(Person person) {
        var potentialMatch =
            personService.findPersonByImportIdentifier(person.getScopusAuthorId());
        if (Objects.nonNull(potentialMatch)) {
            var existingRecordResponse = new rs.teslaris.core.model.person.Person();
            existingRecordResponse.setName(
                new PersonName(potentialMatch.getName().trim(), "", "", null, null));
            existingRecordResponse.setId(potentialMatch.getDatabaseId());
            return existingRecordResponse;
        }

        return null;
    }

    @Nullable
    private PublicationSeriesDTO searchPotentialMatches(DocumentImport documentImport) {
        var potentialMatch =
            journalService.readJournalByIdentifiers(documentImport.getEIssn(),
                documentImport.getPrintIssn(), documentImport.getJournalOpenAlexId());
        if (Objects.nonNull(potentialMatch)) {
            var existingRecordResponse = new PublicationSeriesDTO();
            existingRecordResponse.setTitle(
                List.of(new MultilingualContentDTO(-1, "", potentialMatch.getTitleOther(), 1)));
            existingRecordResponse.setId(potentialMatch.getDatabaseId());
            return existingRecordResponse;
        }

        return null;
    }

    @Nullable
    private Conference searchPotentialMatches(Event conference) {
        var potentialMatch =
            conferenceService.findConferenceByConfId(conference.getConfId());
        if (Objects.nonNull(potentialMatch)) {
            var existingRecordResponse = new Conference();
            existingRecordResponse.setName(potentialMatch.getName());
            existingRecordResponse.setId(potentialMatch.getId());
            return existingRecordResponse;
        }

        return null;
    }

    @NotNull
    private ImportPersonDTO getBasicPersonDTO(Person person) {
        var basicPersonDTO = new ImportPersonDTO();

        var personNameDTO = new PersonNameDTO();
        personNameDTO.setFirstname(person.getName().getFirstName());
        personNameDTO.setOtherName(person.getName().getMiddleName());
        personNameDTO.setLastname(person.getName().getLastName());
        basicPersonDTO.setPersonName(personNameDTO);

        basicPersonDTO.setScopusAuthorId(person.getScopusAuthorId());
        basicPersonDTO.setECrisId(person.getECrisId());
        basicPersonDTO.setENaukaId(person.getENaukaId());
        basicPersonDTO.setOrcid(person.getOrcid());
        basicPersonDTO.setApvnt(person.getApvnt());
        basicPersonDTO.setOpenAlexId(person.getOpenAlexId());
        return basicPersonDTO;
    }

    @Nullable
    private <R> R findAndConvertEntity(Query query, Integer userId, Integer institutionId) {
        var entity = mongoTemplate.findOne(query, DocumentImport.class, "documentImports");

        if (Objects.nonNull(entity)) {
            Method getIdMethod, getIdentifierMethod;

            try {
                getIdentifierMethod = DocumentImport.class.getMethod("getIdentifier");
                getIdMethod = DocumentImport.class.getMethod("getId");
            } catch (NoSuchMethodException e) {
                return null;
            }

            try {
                ProgressReportUtility.updateProgressReport(DataSet.DOCUMENT_IMPORTS,
                    (String) getIdentifierMethod.invoke(entity),
                    (String) getIdMethod.invoke(entity), userId, institutionId,
                    mongoTemplate);
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

    private boolean isLoadedAsUnmanagedEntity(Integer userId, Integer institutionId) {
        if (Objects.isNull(institutionId)) {
            return loadingConfigurationService.getLoadingConfigurationForResearcherUser(userId)
                .getLoadedEntitiesAreUnmanaged();
        } else {
            return loadingConfigurationService.getLoadingConfigurationForAdminUser(institutionId)
                .getLoadedEntitiesAreUnmanaged();
        }
    }
}
