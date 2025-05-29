package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.RecordAlreadyLoadedException;
import rs.teslaris.importer.dto.JournalPublicationLoadDTO;
import rs.teslaris.importer.dto.ProceedingsPublicationLoadDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;
import rs.teslaris.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.importer.service.impl.CommonLoaderImpl;
import rs.teslaris.importer.utility.DataSet;
import rs.teslaris.importer.utility.LoadProgressReport;
import rs.teslaris.importer.utility.ProgressReportUtility;

@SpringBootTest
public class CommonLoaderTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private JournalPublicationConverter journalPublicationConverter;

    @Mock
    private ProceedingsPublicationConverter proceedingsPublicationConverter;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private JournalService journalService;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private ProceedingsService proceedingsService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private PublicationSeriesService publicationSeriesService;

    @Mock
    private PersonService personService;

    @InjectMocks
    private CommonLoaderImpl commonLoader;


    @ParameterizedTest
    @EnumSource(value = DocumentPublicationType.class, names = {"PROCEEDINGS_PUBLICATION",
        "JOURNAL_PUBLICATION"})
    void shouldLoadRecordsWizardWhenDataIsValid(DocumentPublicationType publicationType) {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var expectedQuery = new Query();
        expectedQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        expectedQuery.addCriteria(Criteria.where("is_loaded").is(false));
        expectedQuery.addCriteria(Criteria.where("identifier").gte(lastLoadedId));
        expectedQuery.limit(1);

        var documentImport = new DocumentImport();
        documentImport.setPublicationType(publicationType);
        when(mongoTemplate.findOne(expectedQuery, DocumentImport.class,
            "documentImports")).thenReturn(documentImport);
        when(proceedingsPublicationConverter.toImportDTO(documentImport)).thenReturn(
            new ProceedingsPublicationLoadDTO());
        when(journalPublicationConverter.toImportDTO(documentImport)).thenReturn(
            new JournalPublicationLoadDTO());

        // When
        var result = commonLoader.loadRecordsWizard(userId, null);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldLoadRecordsWizardWhenWithoutProgressReport() {
        // Given
        var userId = 1;

        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(null);

        var expectedQuery = new Query();
        expectedQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        expectedQuery.addCriteria(Criteria.where("is_loaded").is(false));
        expectedQuery.addCriteria(Criteria.where("identifier").gte(""));
        expectedQuery.limit(1);

        var documentImport = new DocumentImport();
        documentImport.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        when(mongoTemplate.findOne(expectedQuery, DocumentImport.class,
            "documentImports")).thenReturn(documentImport);
        when(proceedingsPublicationConverter.toImportDTO(documentImport)).thenReturn(
            new ProceedingsPublicationLoadDTO());

        // When
        var result = commonLoader.loadRecordsWizard(userId, null);

        // Then
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotLoadRecordsWizard_whenNoRecordFound(Boolean importByUser) {
        // Given
        var identifier = 1;

        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, identifier,
            mongoTemplate)).thenReturn(null);

        var expectedQuery = new Query();
        if (importByUser) {
            expectedQuery.addCriteria(Criteria.where("import_users_id").in(identifier));
        } else {
            expectedQuery.addCriteria(Criteria.where("import_institutions_id").in(identifier));
        }
        expectedQuery.addCriteria(Criteria.where("is_loaded").is(false));
        expectedQuery.addCriteria(Criteria.where("identifier").gte(""));
        expectedQuery.limit(1);

        when(mongoTemplate.findOne(expectedQuery, DocumentImport.class,
            "documentImports")).thenReturn(null);

        // When
        Object result;
        if (importByUser) {
            result = commonLoader.loadRecordsWizard(identifier, null);
        } else {
            result = commonLoader.loadRecordsWizard(null, identifier);
        }

        // Then
        assertNull(result);
    }

    @Test
    void shouldMarkRecordAsLoadedSuccessfully() {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var entityClass = DataSet.getClassForValue(DataSet.DOCUMENT_IMPORTS.getStringValue());

        var query = new Query();
        query.addCriteria(Criteria.where("identifier").is(lastLoadedId));
        query.addCriteria(Criteria.where("import_users_id").is(userId));
        query.addCriteria(Criteria.where("loaded").is(false));

        var updateOperation = new Update();
        updateOperation.set("loaded", true);

        doReturn(new DocumentImport()).when(mongoTemplate)
            .findAndModify(eq(query), eq(updateOperation),
                any(FindAndModifyOptions.class), eq(entityClass));

        // When
        commonLoader.markRecordAsLoaded(userId, null);
    }

    @Test
    void shouldThrowExceptionWhenRecordAlreadyLoaded() {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var entityClass = DataSet.getClassForValue(DataSet.DOCUMENT_IMPORTS.getStringValue());

        var query = new Query();
        query.addCriteria(Criteria.where("identifier").is(lastLoadedId));
        query.addCriteria(Criteria.where("import_users_id").is(userId));
        query.addCriteria(Criteria.where("loaded").is(false));

        var updateOperation = new Update();
        updateOperation.set("loaded", true);

        when(mongoTemplate.findAndModify(query, updateOperation,
            new FindAndModifyOptions().returnNew(true).upsert(false), entityClass)).thenReturn(
            null);

        // When
        assertThrows(RecordAlreadyLoadedException.class,
            () -> commonLoader.markRecordAsLoaded(userId, null));

        // Then (RecordAlreadyLoadedException should be thrown)
    }

    @Test
    void shouldSkipRecordSuccessfullyWhenNextRecordExists() {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";
        var nextRecordId = "nextId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        var nextRecord = new DocumentImport();
        nextRecord.setIdentifier(nextRecordId);
        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(nextRecord);

        // When
        commonLoader.skipRecord(userId, null);

        // Then
        assertEquals(nextRecordId, progressReport.getLastLoadedId());
        verify(mongoTemplate).save(progressReport);
        ProgressReportUtility.deleteProgressReport(DataSet.DOCUMENT_IMPORTS, userId, mongoTemplate);
    }

    @Test
    void shouldSkipRecordSuccessfullyWhenNoNextRecordExists() {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(null);

        // When
        commonLoader.skipRecord(userId, null);

        // Then
        assertEquals("", progressReport.getLastLoadedId());
        verify(mongoTemplate).save(progressReport);
        ProgressReportUtility.deleteProgressReport(DataSet.DOCUMENT_IMPORTS, userId, mongoTemplate);
    }

    @Test
    void shouldCountRemainingDocumentsForLoadingSuccessfully() {
        // Given
        var userId = 1;
        var expectedCount = 5;

        var countQuery = new Query();
        countQuery.addCriteria(Criteria.where("loaded").is(false));
        countQuery.addCriteria(Criteria.where("import_users_id").in(userId));

        when(mongoTemplate.count(countQuery, DocumentImport.class)).thenReturn(
            (long) expectedCount);

        // When
        var result = commonLoader.countRemainingDocumentsForLoading(userId, null);

        // Then
        assertEquals(expectedCount, result);
    }

    @Test
    void createInstitutionShouldCreateInstitutionWhenNotExists() {
        // Given
        var scopusAfid = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        var institution = new OrganisationUnit();
        institution.setScopusAfid(scopusAfid);
        var contribution = new PersonDocumentContribution();
        contribution.setInstitutions(List.of(institution));
        currentlyLoadedEntity.setContributions(List.of(contribution));

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gte(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);
        when(organisationUnitService.findOrganisationUnitByScopusAfid(scopusAfid)).thenReturn(null);

        var createdInstitution = new OrganisationUnitDTO();
        when(organisationUnitService.createOrganisationUnit(any(), any())).thenReturn(
            createdInstitution);

        // When
        OrganisationUnitDTO result = commonLoader.createInstitution(scopusAfid, userId, null);

        // Then
        assertEquals(createdInstitution, result);
    }

    @Test
    void createInstitutionShouldThrowNotFoundExceptionWhenNoEntityLoaded() {
        // Given
        var scopusAfid = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.createInstitution(scopusAfid, userId, null));
    }

    @Test
    void createInstitutionShouldThrowNotFoundExceptionWhenInstitutionNotLoaded() {
        // Given
        var scopusAfid = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        currentlyLoadedEntity.setContributions(new ArrayList<>());

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(
            currentlyLoadedEntity);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.createInstitution(scopusAfid, userId, null));
    }

    @Test
    void createPersonShouldCreatePersonWhenNotExists() {
        // Given
        var scopusAuthorId = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        var person = new Person();
        person.setName(new PersonName());
        person.setScopusAuthorId(scopusAuthorId);
        var contribution = new PersonDocumentContribution();
        contribution.setPerson(person);
        currentlyLoadedEntity.setContributions(List.of(contribution));

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gte(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);
        when(personService.readPersonByScopusId(scopusAuthorId)).thenReturn(null);

        var createdPerson = new rs.teslaris.core.model.person.Person();
        createdPerson.setName(new rs.teslaris.core.model.person.PersonName());
        var personalInfo = new PersonalInfo();
        personalInfo.setPostalAddress(new PostalAddress());
        personalInfo.setContact(new Contact());
        createdPerson.setPersonalInfo(personalInfo);
        when(personService.importPersonWithBasicInfo(any(), any())).thenReturn(
            createdPerson);

        // When
        PersonResponseDTO result = commonLoader.createPerson(scopusAuthorId, userId, null);

        // Then
        assertNotNull(result);
    }

    @Test
    void createPersonShouldThrowNotFoundExceptionWhenNoEntityLoaded() {
        // Given
        var scopusAuthorId = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.createPerson(scopusAuthorId, userId, null));
    }

    @Test
    void createPersonShouldThrowNotFoundExceptionWhenPersonNotLoaded() {
        // Given
        var scopusAuthorId = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        currentlyLoadedEntity.setContributions(new ArrayList<>());

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(
            currentlyLoadedEntity);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.createPerson(scopusAuthorId, userId, null));
    }

    @Test
    void createJournalShouldCreateJournalWhenExists() {
        // Given
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        currentlyLoadedEntity.setEIssn(eIssn);

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gte(lastLoadedId));

        var createdJournal = new Journal();
        createdJournal.setId(1);
        when(journalService.createJournal(any(), any())).thenReturn(createdJournal);
        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);

        // When
        var result = commonLoader.createJournal(eIssn, printIssn, userId, null);

        // Then
        assertEquals(createdJournal.getId(), result.getId());
    }

    @Test
    void createJournalShouldThrowNotFoundExceptionWhenNoEntityLoaded() {
        // Given
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";
        var lastLoadedId = "54321";
        var userId = 1;

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.createJournal(eIssn, printIssn, userId, null));
    }

    @Test
    void createJournalShouldThrowNotFoundExceptionWhenJournalNotLoaded() {
        // Given
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(
            currentlyLoadedEntity);

        // When & Then
        assertThrows(
            NotFoundException.class,
            () -> commonLoader.createJournal(eIssn, printIssn, userId, null));
    }

    @Test
    void createProceedingsShouldCreateProceedingsWhenExistsWithPublicationSeries() {
        // Given
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        currentlyLoadedEntity.setEvent(new Event());

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gte(lastLoadedId));

        var createdProceedings = new Proceedings();
        createdProceedings.setId(1);
        var event = new Conference();
        event.setId(1);
        when(conferenceService.createConference(any(), any())).thenReturn(event);
        when(proceedingsService.createProceedings(any(), anyBoolean())).thenReturn(
            createdProceedings);
        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);
        when(publicationSeriesService.findPublicationSeriesByIssn(anyString(),
            anyString())).thenReturn(new Journal());

        // When
        var result = commonLoader.createProceedings(userId, null);

        // Then
        assertEquals(createdProceedings.getId(), result.getId());
    }

    @Test
    void createProceedingsShouldCreateProceedingsWhenExistsWithoutPublicationSeries() {
        // Given
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        currentlyLoadedEntity.setEvent(new Event());

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gte(lastLoadedId));

        var createdProceedings = new Proceedings();
        createdProceedings.setId(1);
        var event = new Conference();
        event.setId(1);
        when(conferenceService.createConference(any(), any())).thenReturn(event);
        when(proceedingsService.createProceedings(any(), anyBoolean())).thenReturn(
            createdProceedings);
        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);

        // When
        var result = commonLoader.createProceedings(userId, null);

        // Then
        assertEquals(createdProceedings.getId(), result.getId());
    }

    @Test
    void createProceedingsShouldThrowNotFoundExceptionWhenNoEntityLoaded() {
        // Given
        var lastLoadedId = "54321";
        var userId = 1;

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedId(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.createProceedings(userId, null));
    }
}
