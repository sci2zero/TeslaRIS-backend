package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.dto.LoadingConfigurationDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.service.impl.CommonHarvesterImpl;
import rs.teslaris.importer.service.impl.worker.DocumentEnrichmentWorker;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
import rs.teslaris.importer.service.interfaces.LoadingConfigurationService;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.service.interfaces.RefManHarvester;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.importer.service.interfaces.WebOfScienceHarvester;
import rs.teslaris.importer.utility.CommonImportUtility;

@SpringBootTest
public class CommonHarvesterTest {

    @Mock
    private ScopusHarvester scopusHarvester;

    @Mock
    private OpenAlexHarvester openAlexHarvester;

    @Mock
    private WebOfScienceHarvester webOfScienceHarvester;

    @Mock
    private BibTexHarvester bibTexHarvester;

    @Mock
    private RefManHarvester refManHarvester;

    @Mock
    private EndNoteHarvester endNoteHarvester;

    @Mock
    private CSVHarvester csvHarvester;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private LoadingConfigurationService loadingConfigurationService;

    @Mock
    private DocumentEnrichmentWorker documentEnrichmentWorker;

    @InjectMocks
    private CommonHarvesterImpl commonHarvester;

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer userId;
    private Integer institutionId;


    @BeforeEach
    void setUp() {
        dateFrom = LocalDate.of(2023, 1, 1);
        dateTo = LocalDate.of(2023, 12, 31);
        userId = 1;
        institutionId = 100;
    }

    @Test
    void shouldReturnZeroWhenPerformHarvestWithUnsupportedUserRole() {
        // given
        String unsupportedRole = "UNSUPPORTED_ROLE";

        // when
        Integer result = commonHarvester.performHarvest(userId, unsupportedRole, dateFrom, dateTo,
            institutionId);

        // then
        assertEquals(0, result);
    }

    @Test
    void shouldPerformHarvestForResearcherRole() {
        // given
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 5));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 3));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 2));

        when(scopusHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(wosResults);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }}));

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.RESEARCHER.name(), dateFrom, dateTo,
                institutionId);

        // then
        assertEquals(10, result);
        verify(scopusHarvester).harvestDocumentsForAuthor(userId, dateFrom, dateTo,
            new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForAuthor(userId, dateFrom, dateTo,
            new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForAuthor(userId, dateFrom, dateTo,
            new HashMap<>());
        verify(notificationService).createNotification(any());
    }

    @Test
    void shouldPerformHarvestForInstitutionalEditorRole() {
        // given
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 4));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 2));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 1));

        when(scopusHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(null),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(null),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(null),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(wosResults);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }}));

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.INSTITUTIONAL_EDITOR.name(), dateFrom,
                dateTo, institutionId);

        // then
        assertEquals(7, result);
        verify(scopusHarvester).harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
            dateTo, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
            dateTo, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitutionalEmployee(userId, null,
            dateFrom, dateTo, new HashMap<>());
    }

    @Test
    void shouldPerformHarvestForAdminRole() {
        // given
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 6));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 4));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 3));

        when(scopusHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId),
            eq(institutionId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId),
            eq(institutionId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(wosResults);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }}));

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.ADMIN.name(), dateFrom, dateTo,
                institutionId);

        // then
        assertEquals(13, result);
        verify(scopusHarvester).harvestDocumentsForInstitutionalEmployee(userId, institutionId,
            dateFrom, dateTo, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitutionalEmployee(userId, institutionId,
            dateFrom, dateTo, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitutionalEmployee(userId,
            institutionId, dateFrom, dateTo, new HashMap<>());
    }

    @Test
    void shouldReturnZeroWhenNoDocumentsImported() {
        // given
        when(scopusHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(new HashMap<>());
        when(openAlexHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(new HashMap<>());
        when(webOfScienceHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(new HashMap<>());
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }}));

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.RESEARCHER.name(), dateFrom, dateTo,
                institutionId);

        // then
        assertEquals(0, result);
        verify(notificationService).createNotification(any());
    }

    @Test
    void shouldReturnZeroWhenPerformAuthorCentricHarvestWithUnsupportedUserRole() {
        // given
        String unsupportedRole = "UNSUPPORTED_ROLE";
        List<Integer> authorIds = List.of(1, 2, 3);

        // when
        Integer result =
            commonHarvester.performAuthorCentricHarvest(userId, unsupportedRole, dateFrom, dateTo,
                authorIds, false, institutionId);

        // then
        assertEquals(0, result);
    }

    @Test
    void shouldPerformAuthorCentricHarvestForInstitutionalEditorRole() {
        // given
        List<Integer> authorIds = List.of(1, 2, 3);
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 5));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 3));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 2));

        when(scopusHarvester.harvestDocumentsForInstitution(eq(userId), eq(null), eq(dateFrom),
            eq(dateTo), eq(authorIds), eq(false), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitution(eq(userId), eq(null), eq(dateFrom),
            eq(dateTo), eq(authorIds), eq(false), any()))
            .thenReturn(openAlexResults);
        when(
            webOfScienceHarvester.harvestDocumentsForInstitution(eq(userId), eq(null), eq(dateFrom),
                eq(dateTo), eq(authorIds), eq(false), any()))
            .thenReturn(wosResults);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }}));

        // when
        Integer result = commonHarvester.performAuthorCentricHarvest(userId,
            UserRole.INSTITUTIONAL_EDITOR.name(), dateFrom, dateTo, authorIds, false,
            institutionId);

        // then
        assertEquals(10, result);
        verify(scopusHarvester).harvestDocumentsForInstitution(userId, null, dateFrom, dateTo,
            authorIds, false, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitution(userId, null, dateFrom, dateTo,
            authorIds, false, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitution(userId, null, dateFrom, dateTo,
            authorIds, false, new HashMap<>());
    }

    @Test
    void shouldPerformAuthorCentricHarvestForAdminRole() {
        // given
        List<Integer> authorIds = List.of(1, 2, 3);
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 7));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 4));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 3));

        when(scopusHarvester.harvestDocumentsForInstitution(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), eq(authorIds), eq(true), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitution(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), eq(authorIds), eq(true), any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForInstitution(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), eq(authorIds), eq(true), any()))
            .thenReturn(wosResults);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }}));

        // when
        Integer result =
            commonHarvester.performAuthorCentricHarvest(userId, UserRole.ADMIN.name(), dateFrom,
                dateTo, authorIds, true, institutionId);

        // then
        assertEquals(14, result);
        verify(scopusHarvester).harvestDocumentsForInstitution(userId, institutionId, dateFrom,
            dateTo, authorIds, true, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitution(userId, institutionId, dateFrom,
            dateTo, authorIds, true, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitution(userId, institutionId,
            dateFrom, dateTo, authorIds, true, new HashMap<>());
    }

    @Test
    void shouldProcessBibTexFile() {
        // given
        String filename = "publications.bib";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(bibTexHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldProcessRisFile() {
        // given
        String filename = "publications.ris";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(refManHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldProcessEndNoteFile() {
        // given
        String filename = "publications.enw";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(endNoteHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldProcessCsvFile() {
        // given
        String filename = "publications.csv";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(csvHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldReturnNullWhenDocumentNotFound() {
        // given
        Integer documentId = 1;

        when(documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // when
        String result = commonHarvester.performDocumentCentricHarvest(documentId);

        // then
        assertNull(result);
        verify(documentPublicationIndexRepository)
            .findDocumentPublicationIndexByDatabaseId(documentId);
    }

    @Test
    void shouldFetchOnlyJournalAndProceedingsWhenAutoupdateFalse() {
        // given
        List<Integer> institutionIds = List.of(1, 2);

        when(documentPublicationIndexRepository.fetchForInstitutionsAndTypes(
            eq(institutionIds),
            eq(List.of(
                DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                DocumentPublicationType.PROCEEDINGS_PUBLICATION.name()
            )),
            any()))
            .thenReturn(Page.empty());
        when(loadingConfigurationService.getLoadingConfigurationForInstitution(
            any())).thenReturn(new LoadingConfigurationDTO());

        // when
        commonHarvester.enrichMetadataForInstitution(institutionIds, false);

        // then
        verify(documentPublicationIndexRepository)
            .fetchForInstitutionsAndTypes(
                eq(institutionIds),
                eq(List.of(
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    DocumentPublicationType.PROCEEDINGS_PUBLICATION.name()
                )),
                any());
        verify(loadingConfigurationService, times(2))
            .saveLoadingConfiguration(any(), any());
    }

    @Test
    void shouldPerformHarvestWhenDoiExists() {
        // given
        var documentId = 1;
        var doi = "10.1234/test";

        var index = new DocumentPublicationIndex();
        index.setDoi(doi);
        index.setAuthorIds(List.of());
        index.setOrganisationUnitIdsActive(List.of());

        when(documentPublicationIndexRepository
            .findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(index));

        var harvested = new DocumentImport();
        harvested.setDoi(doi);

        when(scopusHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.of(harvested));
        when(openAlexHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.empty());
        when(webOfScienceHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.empty());

        try (var mockedStatic = mockStatic(CommonImportUtility.class)) {

            mockedStatic.when(() ->
                    CommonImportUtility.findImportByDOIOrMetadata(any()))
                .thenReturn(null);

            mockedStatic.when(() ->
                    CommonImportUtility.generateEmbedding(any()))
                .thenReturn(null);

            mockedStatic.when(CommonImportUtility::getAdminUserIds)
                .thenReturn(Set.of(1));

            var savedImport = new DocumentImport();
            savedImport.setId("mongo-id");

            when(mongoTemplate.save(any(), eq("documentImports")))
                .thenReturn(savedImport);

            // when
            var result = commonHarvester.performDocumentCentricHarvest(documentId);

            // then
            assertEquals("mongo-id", result);
            verify(scopusHarvester).harvestDocumentForDoi(doi);
            verify(openAlexHarvester).harvestDocumentForDoi(doi);
            verify(webOfScienceHarvester).harvestDocumentForDoi(doi);
            verify(mongoTemplate).save(any(), eq("documentImports"));
        }
    }

    @Test
    void shouldScanEachFetchedDocument() {
        // given
        var institutionIds = List.of(1);
        var doi = "10.5555/test";

        var index = new DocumentPublicationIndex();
        index.setDoi(doi);
        index.setAuthorIds(List.of());
        index.setOrganisationUnitIdsActive(List.of());

        var page = new PageImpl<>(List.of(index));

        when(documentPublicationIndexRepository.fetchForInstitutionsAndTypes(
            any(), any(), any()))
            .thenReturn(page);

        var harvested = new DocumentImport();
        harvested.setDoi(doi);

        when(scopusHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.of(harvested));
        when(openAlexHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.empty());
        when(webOfScienceHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.empty());

        try (var mockedStatic = mockStatic(CommonImportUtility.class)) {

            mockedStatic.when(() ->
                    CommonImportUtility.findImportByDOIOrMetadata(any()))
                .thenReturn(null);

            mockedStatic.when(() ->
                    CommonImportUtility.generateEmbedding(any()))
                .thenReturn(null);

            mockedStatic.when(CommonImportUtility::getAdminUserIds)
                .thenReturn(Set.of(1));

            when(mongoTemplate.save(any(), eq("documentImports")))
                .thenReturn(new DocumentImport());
            when(loadingConfigurationService.getLoadingConfigurationForInstitution(
                any())).thenReturn(new LoadingConfigurationDTO());

            // when
            commonHarvester.enrichMetadataForInstitution(institutionIds, false);

            // then
            verify(scopusHarvester).harvestDocumentForDoi(doi);
            verify(mongoTemplate).save(any(), eq("documentImports"));
            verify(loadingConfigurationService).saveLoadingConfiguration(any(), any());
        }
    }

    @Test
    void shouldCallEnrichmentWorkerWhenAutoupdateTrueThroughPublicMethod() {
        // given
        var institutionIds = List.of(1);
        var doi = "10.9999/test";
        var documentId = 55;

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(documentId);
        index.setDoi(doi);
        index.setAuthorIds(List.of());
        index.setOrganisationUnitIdsActive(List.of());

        var harvested = new DocumentImport();
        harvested.setDoi(doi);

        when(scopusHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.of(harvested));
        when(openAlexHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.empty());
        when(webOfScienceHarvester.harvestDocumentForDoi(doi))
            .thenReturn(Optional.empty());

        when(loadingConfigurationService.getLoadingConfigurationForInstitution(any()))
            .thenReturn(new LoadingConfigurationDTO());

        try (var functionalMock = mockStatic(FunctionalUtil.class);
             var commonImportMock = mockStatic(CommonImportUtility.class)) {

            commonImportMock.when(() ->
                    CommonImportUtility.generateEmbedding(any()))
                .thenReturn(null);

            commonImportMock.when(() ->
                    CommonImportUtility.findImportByDOIOrMetadata(any()))
                .thenReturn(null);

            functionalMock.when(() ->
                    FunctionalUtil.performBulkOperation(any(), any(), any()))
                .thenAnswer(invocation -> {
                    var consumer =
                        (Consumer<DocumentPublicationIndex>) invocation.getArgument(2);
                    consumer.accept(index);
                    return null;
                });

            // when
            commonHarvester.enrichMetadataForInstitution(institutionIds, true);

            // then
            verify(documentEnrichmentWorker)
                .enrichDocumentMetadata(eq(documentId), any(DocumentImport.class));

            verify(loadingConfigurationService)
                .saveLoadingConfiguration(eq(1), any(LoadingConfigurationDTO.class));
        }
    }
}
