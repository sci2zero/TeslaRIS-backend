package rs.teslaris.importer.service.impl;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.RecordAlreadyLoadedException;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.importer.service.interfaces.CommonLoader;
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

    @NotNull
    private static ImportPersonDTO getBasicPersonDTO(Person person) {
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
        return basicPersonDTO;
    }

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
    public <R> R loadSkippedRecordsWizard(Integer userId, Integer institutionId) {
        ProgressReportUtility.resetProgressReport(DataSet.DOCUMENT_IMPORTS, userId, mongoTemplate);
        return loadRecordsWizard(userId, institutionId);
    }

    @Override
    public void skipRecord(Integer userId, Integer institutionId) {
        var progressReport =
            Objects.requireNonNullElse(
                ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                    mongoTemplate), new LoadProgressReport("1", userId, DataSet.DOCUMENT_IMPORTS));
        Query nextRecordQuery = new Query();
        if (Objects.nonNull(institutionId)) {
            nextRecordQuery.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        } else {
            nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        }
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
    public void markRecordAsLoaded(Integer userId, Integer institutionId) {
        var progressReport =
            Objects.requireNonNullElse(
                ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
                    mongoTemplate), new LoadProgressReport("1", userId, DataSet.DOCUMENT_IMPORTS));

        Query query = new Query();
        query.addCriteria(Criteria.where("identifier").is(progressReport.getLastLoadedId()));
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
    public OrganisationUnitDTO createInstitution(String scopusAfid, Integer userId,
                                                 Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

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
    public PersonResponseDTO createPerson(String scopusAuthorId, Integer userId,
                                          Integer institutionId) {
        var currentlyLoadedEntity = retrieveCurrentlyLoadedEntity(userId, institutionId);

        if (Objects.isNull(currentlyLoadedEntity)) {
            throw new NotFoundException("No entity is being loaded at the moment.");
        }

        for (var contribution : currentlyLoadedEntity.getContributions()) {
            if (contribution.getPerson().getScopusAuthorId().equals(scopusAuthorId) &&
                Objects.isNull(personService.findPersonByScopusAuthorId(scopusAuthorId))) {

                var savedPerson = createLoadedPerson(contribution.getPerson());

                var pastContributionInstitutionIdentifiers = new HashSet<String>();
                contribution.getInstitutions().forEach(institution -> {
                    if (pastContributionInstitutionIdentifiers.contains(
                        institution.getScopusAfid())) {
                        return;
                    }

                    var institutionIndex =
                        organisationUnitService.findOrganisationUnitByScopusAfid(
                            institution.getScopusAfid());

                    if (Objects.nonNull(institutionIndex)) {
                        var employmentInstitution =
                            organisationUnitService.findOne(institutionIndex.getDatabaseId());
                        var currentEmployment =
                            new Employment(null, null, ApproveStatus.APPROVED, new HashSet<>(),
                                InvolvementType.EMPLOYED_AT, new HashSet<>(), null,
                                employmentInstitution, null, new HashSet<>());
                        savedPerson.addInvolvement(currentEmployment);

                        pastContributionInstitutionIdentifiers.add(institution.getScopusAfid());
                    }
                });

                personService.save(savedPerson);
                personService.indexPerson(savedPerson, savedPerson.getId());

                return PersonConverter.toDTO(savedPerson);
            }
        }

        throw new NotFoundException("Person with given ScopusID is not loaded.");
    }

    public void updatePersonInvolvements(DocumentImport currentlyLoadedEntity) {
        for (var contribution : currentlyLoadedEntity.getContributions()) {
            var scopusAuthorId = contribution.getPerson().getScopusAuthorId();

            var savedPersonIndex = personService.findPersonByScopusAuthorId(scopusAuthorId);
            var savedPerson = personService.findOne(savedPersonIndex.getDatabaseId());

            contribution.getInstitutions().forEach(institution -> {
                var institutionIndex =
                    organisationUnitService.findOrganisationUnitByScopusAfid(
                        institution.getScopusAfid());

                if (savedPerson.getInvolvements().stream().anyMatch(
                    i -> (i.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                        i.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                        Objects.nonNull(i.getOrganisationUnit()) &&
                        i.getOrganisationUnit().getId()
                            .equals(institutionIndex.getDatabaseId()))) {
                    return;
                }

                if (Objects.nonNull(institutionIndex)) {
                    var employmentInstitution =
                        organisationUnitService.findOne(institutionIndex.getDatabaseId());
                    var currentEmployment =
                        new Employment(null, null, ApproveStatus.APPROVED, new HashSet<>(),
                            InvolvementType.EMPLOYED_AT, new HashSet<>(), null,
                            employmentInstitution, null, new HashSet<>());
                    savedPerson.addInvolvement(currentEmployment);
                }
            });

            personService.save(savedPerson);
            personService.indexPerson(savedPerson, savedPerson.getId());
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
                currentlyLoadedEntity.getPrintIssn().equals(printIssn))) {
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

    private DocumentImport retrieveCurrentlyLoadedEntity(Integer userId, Integer institutionId) {
        Query query = new Query();
        if (Objects.nonNull(institutionId)) {
            query.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        } else {
            query.addCriteria(Criteria.where("import_users_id").in(userId));
        }
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
            organisationUnitService.findOrganisationUnitByScopusAfid(institution.getScopusAfid());
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
            personService.findPersonByScopusAuthorId(person.getScopusAuthorId());
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
            journalService.readJournalByIssn(documentImport.getEIssn(),
                documentImport.getPrintIssn());
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
