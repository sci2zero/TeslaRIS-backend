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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.model.person.PostalAddress;
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
import rs.teslaris.importer.dto.JournalPublicationLoadDTO;
import rs.teslaris.importer.dto.LoadingConfigurationDTO;
import rs.teslaris.importer.dto.ProceedingsPublicationLoadDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;
import rs.teslaris.importer.model.converter.load.publication.JournalPublicationConverter;
import rs.teslaris.importer.model.converter.load.publication.ProceedingsPublicationConverter;
import rs.teslaris.importer.service.impl.CommonLoaderImpl;
import rs.teslaris.importer.service.interfaces.LoadingConfigurationService;
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

    @Mock
    private LoadingConfigurationService loadingConfigurationService;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @InjectMocks
    private CommonLoaderImpl commonLoader;


    private static Stream<Arguments> argumentSources() {
        return Stream.of(
            Arguments.of(1, true),
            Arguments.of(1, false),
            Arguments.of(null, true),
            Arguments.of(null, false),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @EnumSource(value = DocumentPublicationType.class, names = {"PROCEEDINGS_PUBLICATION",
        "JOURNAL_PUBLICATION"})
    void shouldLoadRecordsWizardWhenDataIsValid(DocumentPublicationType publicationType) {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var expectedQuery = new Query();
        expectedQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        expectedQuery.addCriteria(Criteria.where("is_loaded").is(false));
        expectedQuery.addCriteria(Criteria.where("_id").gte(progressReport.getLastLoadedId()));
        expectedQuery.limit(1);

        var documentImport = new DocumentImport();
        documentImport.setPublicationType(publicationType);
        documentImport.setId(ProgressReportUtility.DEFAULT_HEX_ID);

        if (publicationType.equals(DocumentPublicationType.JOURNAL_PUBLICATION)) {
            documentImport.setPublishedIn(List.of(new MultilingualContent()));
        } else {
            documentImport.setEvent(new Event() {{
                setName(List.of(new MultilingualContent()));
            }});
        }

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

        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(null);

        var expectedQuery = new Query();
        expectedQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        expectedQuery.addCriteria(Criteria.where("is_loaded").is(false));
        expectedQuery.addCriteria(Criteria.where("identifier").gte(""));
        expectedQuery.limit(1);

        var documentImport = new DocumentImport();
        documentImport.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        documentImport.setId(ProgressReportUtility.DEFAULT_HEX_ID);
        documentImport.setEvent(new Event() {{
            setName(List.of(new MultilingualContent()));
        }});
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
            identifier,
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

    @ParameterizedTest
    @MethodSource("argumentSources")
    void shouldMarkRecordAsLoadedSuccessfully(Integer oldPublicationId,
                                              Boolean deleteOldPublication) {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        if (Objects.nonNull(oldPublicationId) && Objects.nonNull(deleteOldPublication)) {
            when(documentPublicationService.findDocumentDuplicates(any(), any(), any(),
                any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(new DocumentPublicationIndex() {{
                    setDatabaseId(1);
                }})));
            when(documentPublicationService.findDocumentById(1)).thenReturn(
                new JournalPublication());
        }

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
        commonLoader.markRecordAsLoaded(userId, null, oldPublicationId, deleteOldPublication, null);

        if (Objects.nonNull(oldPublicationId) && Objects.nonNull(deleteOldPublication)) {
            verify(documentPublicationService, times(1)).findDocumentDuplicates(any(), any(),
                any(), any(), any(), any());
            if (deleteOldPublication) {
                verify(documentPublicationService, times(1)).deleteDocumentPublication(1);
            } else {
                verify(documentPublicationService, times(1)).findDocumentById(1);
                verify(documentPublicationService, times(1)).save(any());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    void shouldThrowExceptionWhenRecordAlreadyLoaded(Integer oldPublicationId,
                                                     Boolean deleteOldPublication) {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
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
            () -> commonLoader.markRecordAsLoaded(userId, null, oldPublicationId,
                deleteOldPublication, null));

        // Then (RecordAlreadyLoadedException should be thrown)
    }

    @Test
    void shouldSkipRecordSuccessfullyWhenNextRecordExists() {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";
        var nextRecordId = "nextId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("_id").gt(progressReport.getLastLoadedId()));

        var nextRecord = new DocumentImport();
        nextRecord.setIdentifier(nextRecordId);
        nextRecord.setId(ProgressReportUtility.DEFAULT_HEX_ID);
        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(nextRecord);

        // When
        commonLoader.skipRecord(userId, null, false);

        // Then
        assertEquals(nextRecordId, progressReport.getLastLoadedIdentifier());
        verify(mongoTemplate).save(progressReport);
        ProgressReportUtility.deleteProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate);
    }

    @Test
    void shouldSkipRecordSuccessfullyWhenNoNextRecordExists() {
        // Given
        var userId = 1;
        var lastLoadedId = "someId";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(null);

        // When
        commonLoader.skipRecord(userId, null, false);

        // Then
        assertEquals(ProgressReportUtility.DEFAULT_HEX_ID,
            progressReport.getLastLoadedId().toHexString());
        verify(mongoTemplate).save(progressReport);
        ProgressReportUtility.deleteProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate);
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
        institution.setImportId(scopusAfid);
        var contribution = new PersonDocumentContribution();
        contribution.setInstitutions(Set.of(institution));
        currentlyLoadedEntity.setContributions(List.of(contribution));

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);
        when(organisationUnitService.findOrganisationUnitByImportId(scopusAfid)).thenReturn(null);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, false, true));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, false, false));
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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").gt(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class)).thenReturn(
            currentlyLoadedEntity);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, false, false));

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
        person.setImportId(scopusAuthorId);
        var contribution = new PersonDocumentContribution();
        contribution.setPerson(person);
        currentlyLoadedEntity.setContributions(List.of(contribution));

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, false, true));

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, false, false));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, false, false));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

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
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
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

    @Test
    void shouldReturnPersonResponseDTOWhenUnmanagedAndPersonMatchesScopusId() {
        // Given
        var scopusAuthorId = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        var person = new Person();
        person.setName(new PersonName());
        person.setImportId(scopusAuthorId);
        var contribution = new PersonDocumentContribution();
        contribution.setPerson(person);
        currentlyLoadedEntity.setContributions(List.of(contribution));

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(userId))
            .thenReturn(new LoadingConfigurationDTO(true, true, false));

        when(personService.findPersonByImportIdentifier(scopusAuthorId)).thenReturn(null);

        // When
        var response = commonLoader.createPerson(scopusAuthorId, userId, null);

        // Then
        assertNotNull(response);
        assertNotNull(response.getPersonName());

        verify(personService, never()).save(any());
        verify(personService, never()).indexPerson(any());
    }

    @Test
    void createInstitutionShouldCreateUnmanagedInstitutionWhenNotExists() {
        // Given
        var scopusAfid = "12345";
        var lastLoadedId = "54321";
        var userId = 1;

        var currentlyLoadedEntity = new DocumentImport();
        var institution = new OrganisationUnit();
        institution.setImportId(scopusAfid);
        var contribution = new PersonDocumentContribution();
        contribution.setInstitutions(Set.of(institution));
        currentlyLoadedEntity.setContributions(List.of(contribution));

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);
        when(organisationUnitService.findOrganisationUnitByImportId(scopusAfid)).thenReturn(null);
        when(loadingConfigurationService.getLoadingConfigurationForResearcherUser(
            userId)).thenReturn(new LoadingConfigurationDTO(true, true, false));

        // When
        var result = commonLoader.createInstitution(scopusAfid, userId, null);

        // Then
        assertNull(result.getId());
        verify(organisationUnitService, never()).createOrganisationUnit(any(), anyBoolean());
    }

    @Test
    void updateManuallySelectedPersonIdentifiersShouldThrowWhenNoEntityLoaded() {
        // Given
        var userId = 1;
        var institutionId = 2;
        var lastLoadedId = "1234";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            null);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.updateManuallySelectedPersonIdentifiers("imp1", 1, userId,
                institutionId));
    }

    @Test
    void updateManuallySelectedInstitutionIdentifiersShouldThrowWhenNoEntityLoaded() {
        // Given
        var userId = 1;
        var institutionId = 2;
        var lastLoadedId = "1234";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            null);

        // When & Then
        assertThrows(NotFoundException.class,
            () -> commonLoader.updateManuallySelectedInstitutionIdentifiers("imp1", 1, userId,
                institutionId));
    }

    @Test
    void updateManuallySelectedPublicationSeriesIdentifiersShouldThrowWhenNoEntityLoaded() {
        // Given
        var userId = 1;
        var institutionId = 2;
        var lastLoadedId = "1234";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));

        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate))
            .thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class, "documentImports"))
            .thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> commonLoader
            .updateManuallySelectedPublicationSeriesIdentifiers("1234-5678", null, 1, userId,
                institutionId));
    }

    @Test
    void updateManuallySelectedConferenceIdentifiersShouldThrowWhenNoEntityLoaded() {
        // Given
        var userId = 1;
        var institutionId = 2;
        var lastLoadedId = "1234";

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));

        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, null,
            mongoTemplate))
            .thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_users_id").in(userId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class, "documentImports"))
            .thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> commonLoader
            .updateManuallySelectedConferenceIdentifiers(1, userId, institutionId));
    }

    @Test
    void prepareOldDocumentForOverwritingShouldClearIdentifiersWhenOldDocumentIsInDuplicates() {
        // Given
        var userId = 1;
        var institutionId = 2;
        var oldDocumentId = 1;
        var lastLoadedId = "54321";

        var currentlyLoadedEntity = new DocumentImport();

        var progressReport = new LoadProgressReport();
        progressReport.setLastLoadedIdentifier(lastLoadedId);
        progressReport.setLastLoadedId(new ObjectId(ProgressReportUtility.DEFAULT_HEX_ID));
        when(
            ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, userId, institutionId,
                mongoTemplate)).thenReturn(progressReport);

        var nextRecordQuery = new Query();
        nextRecordQuery.addCriteria(Criteria.where("import_institutions_id").in(institutionId));
        nextRecordQuery.addCriteria(Criteria.where("is_loaded").is(false));
        nextRecordQuery.addCriteria(Criteria.where("identifier").is(lastLoadedId));

        when(mongoTemplate.findOne(nextRecordQuery, DocumentImport.class,
            "documentImports")).thenReturn(
            currentlyLoadedEntity);

        var mockDoc = new JournalPublication();
        mockDoc.setId(oldDocumentId);
        mockDoc.setDoi("some-doi");
        mockDoc.setScopusId("some-scopus");
        mockDoc.setOpenAlexId("some-openalex");
        when(documentPublicationService.findDocumentDuplicates(any(), any(), any(),
            any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new DocumentPublicationIndex() {{
                setDatabaseId(1);
            }})));

        when(documentPublicationService.findOne(oldDocumentId)).thenReturn(mockDoc);

        // When
        commonLoader.prepareOldDocumentForOverwriting(userId, institutionId, oldDocumentId);

        // Then
        assertEquals("", mockDoc.getDoi());
        assertEquals("", mockDoc.getScopusId());
        assertEquals("", mockDoc.getOpenAlexId());
        verify(documentPublicationService).save(mockDoc);
    }

    @Test
    void prepareOldDocumentForOverwritingShouldThrowWhenNoEntityLoaded() {
        // Given
        when(ProgressReportUtility.getProgressReport(DataSet.DOCUMENT_IMPORTS, 1, 2,
            mongoTemplate)).thenReturn(null);

        // Then
        assertThrows(NotFoundException.class, () ->
            commonLoader.prepareOldDocumentForOverwriting(1, 2, 100));
    }
}
