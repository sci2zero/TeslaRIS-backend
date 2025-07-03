package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.DocumentPublicationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;
import rs.teslaris.core.util.search.SearchRequestType;

@SpringBootTest
public class DocumentPublicationServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private JournalService journalService;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @Mock
    private ExpressionTransformer expressionTransformer;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private SearchFieldsLoader searchFieldsLoader;

    @InjectMocks
    private DocumentPublicationServiceImpl documentPublicationService;

    static Stream<Arguments> shouldFindDocumentPublicationsWhenSearchingWithSimpleQuery() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(1, null),
            Arguments.of(null, 1),
            Arguments.of(1, 1)
        );
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(documentPublicationService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldReadDocumentPublicationWhenItExists() {
        // given
        var expected = new MonographPublication();
        expected.setId(1);
        when(documentRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = documentPublicationService.readDocumentPublication(1);

        // then
        assertEquals(expected.getId(), result.getId());
    }

    @Test
    public void shouldReturnDocumentWhenItExists() {
        // given
        var expected = new MonographPublication();
        when(documentRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = documentPublicationService.findOne(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenDocumentDoesNotExist() {
        // given
        when(documentRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> documentPublicationService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldDeleteDocumentFileWithProof() {
        // Given
        var documentId = 1;
        var documentFileId = 1;
        var isProof = true;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.findOne(documentFileId)).thenReturn(documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.deleteDocumentFile(documentId, documentFileId);

        // Then
        verify(documentFileService, times(1)).deleteDocumentFile(any());
    }

    @Test
    public void shouldDeleteDocumentFileWithFileItem() {
        // Given
        var documentId = 1;
        var documentFileId = 1;
        var isProof = false;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.findOne(documentFileId)).thenReturn(documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.deleteDocumentFile(documentId, documentFileId);

        // Then
        verify(documentFileService, times(1)).deleteDocumentFile(any());
    }

    @Test
    public void shouldAddDocumentFileWithProof() {
        // Given
        var documentId = 1;
        var isProof = true;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.saveNewPublicationDocument(any(DocumentFileDTO.class), eq(false),
            eq(document))).thenReturn(
            documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.addDocumentFile(documentId, new DocumentFileDTO(), isProof);

        // Then
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    public void shouldAddDocumentFileWithFileItem() {
        // Given
        var documentId = 1;
        var isProof = false;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.saveNewPublicationDocument(any(DocumentFileDTO.class),
            eq(!isProof), eq(document))).thenReturn(
            documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));
        when(documentFileService.findDocumentFileIndexByDatabaseId(any())).thenReturn(
            new DocumentFileIndex());

        // When
        documentPublicationService.addDocumentFile(documentId, new DocumentFileDTO(), isProof);

        // Then
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    public void shouldUpdateDocumentApprovalStatusToApproved() {
        // Given
        var documentId = 1;
        var isApproved = true;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        // When
        documentPublicationService.updateDocumentApprovalStatus(documentId, isApproved);

        // Then
        assertEquals(ApproveStatus.APPROVED, document.getApproveStatus());
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    public void shouldUpdateDocumentApprovalStatusToDeclined() {
        // Given
        var documentId = 1;
        var isApproved = false;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        // When
        documentPublicationService.updateDocumentApprovalStatus(documentId, isApproved);

        // Then
        assertEquals(ApproveStatus.DECLINED, document.getApproveStatus());
        verify(documentRepository, times(1)).save(document);
    }

    @ParameterizedTest
    @MethodSource("shouldFindDocumentPublicationsWhenSearchingWithSimpleQuery")
    public void shouldFindDocumentPublicationsWhenSearchingWithSimpleQuery(Integer institutionId,
                                                                           Integer commissionId) {
        // given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // when
        var result =
            documentPublicationService.searchDocumentPublications(new ArrayList<>(tokens),
                pageable, SearchRequestType.SIMPLE, institutionId, commissionId, null);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldFindDocumentPublicationsWhenSearchingWithAdvancedQuery() {
        // given
        var tokens = List.of("keyword_sr:ključna ријеч");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // when
        var result =
            documentPublicationService.searchDocumentPublications(new ArrayList<>(tokens),
                pageable, SearchRequestType.ADVANCED, null, null, new ArrayList<>());

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldGetDocumentPublicationCount() {
        // Given
        var expectedCount = 42L;
        when(documentPublicationIndexRepository.count()).thenReturn(expectedCount);

        // When
        long actualCount = documentPublicationService.getPublicationCount();

        // Then
        assertEquals(expectedCount, actualCount);
        verify(documentPublicationIndexRepository, times(1)).count();
    }

    @Test
    public void testDeleteDocumentPublication() {
        // Given
        var documentId = 1;
        when(documentRepository.findById(documentId)).thenReturn(
            Optional.of(new JournalPublication()));
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.deleteDocumentPublication(documentId);

        // Then
        verify(documentRepository, times(1)).delete(any());
        verify(documentPublicationIndexRepository, times(1)).delete(any());
    }

    @Test
    public void shouldNotRemoveDocumentPublicationIndexWhenNotIndexed() {
        // Given
        var documentId = 1;

        when(documentRepository.findById(documentId)).thenReturn(
            Optional.of(new JournalPublication()));
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // When
        documentPublicationService.deleteDocumentPublication(documentId);

        // Then
        verify(documentRepository, times(1)).delete(any());
        verify(documentPublicationIndexRepository, never()).delete(any());
    }

    @Test
    public void shouldDeleteIndexes() {
        // Given: No need for any specific arrangement

        // When
        documentPublicationService.deleteIndexes();

        // Then
        verify(documentPublicationIndexRepository, times(1)).deleteAll();
    }

    @Test
    public void shouldFindResearcherPublications() {
        // given
        var authorId = 123;
        var pageable = PageRequest.of(0, 10);
        var expectedPage = new PageImpl<>(List.of(new DocumentPublicationIndex()));
        when(documentPublicationIndexRepository.findByAuthorIdsAndDatabaseIdNotIn(anyInt(), any(),
            any(Pageable.class))).thenReturn(expectedPage);

        // when
        var resultPage =
            documentPublicationService.findResearcherPublications(authorId, List.of(), pageable);

        // then
        assertEquals(expectedPage, resultPage);
        verify(documentPublicationIndexRepository).findByAuthorIdsAndDatabaseIdNotIn(authorId,
            List.of(), pageable);
    }

    @Test
    public void shouldReadDocumentForPublisherWhenItExists() {
        // given
        var expected = new DocumentPublicationIndex();
        var pageable = Pageable.ofSize(5);
        when(documentPublicationIndexRepository.findByPublisherId(1, pageable)).thenReturn(
            new PageImpl<>(List.of(expected)));

        // when
        var result = documentPublicationService.findPublicationsForPublisher(1, pageable);

        // then
        assertEquals(1, result.getTotalElements());
    }

    @Test
    public void shouldFindDocumentDuplicates() {
        // given
        var titles = Arrays.asList("titleSr", "titleEn", "titleRu");

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // when
        var result =
            documentPublicationService.findDocumentDuplicates(titles, "DOI", "scopusId",
                "openAlexId");

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    void shouldDoNothingWhenContributionDoesNotExist() {
        // Given
        var personId = 1;
        var documentId = 1;
        when(personContributionService.findContributionForResearcherAndDocument(personId,
            documentId))
            .thenReturn(null);

        // When
        documentPublicationService.unbindResearcherFromContribution(personId, documentId);

        // Then
        verify(personContributionService, never()).save(any());
        verify(documentRepository, never()).findById(any());
        verify(documentPublicationIndexRepository,
            never()).findDocumentPublicationIndexByDatabaseId(any());
    }

    @Test
    void shouldUpdateIndexWhenDocumentAndIndexExist() {
        // Given
        var personId = 1;
        var documentId = 1;

        var contribution = new PersonDocumentContribution();
        var document = new Software();
        var index = new DocumentPublicationIndex();

        when(personContributionService.findContributionForResearcherAndDocument(personId,
            documentId))
            .thenReturn(contribution);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(index));

        // When
        documentPublicationService.unbindResearcherFromContribution(personId, documentId);

        // Then
        verify(documentRepository).findById(documentId);
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseId(
            documentId);
    }

    @Test
    void shouldNotUpdateIndexWhenDocumentDoesNotExist() {
        // Given
        var personId = 1;
        var documentId = 1;

        var contribution = new PersonDocumentContribution();

        when(personContributionService.findContributionForResearcherAndDocument(personId,
            documentId))
            .thenReturn(contribution);
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When
        documentPublicationService.unbindResearcherFromContribution(personId, documentId);

        // Then
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldNotUpdateIndexWhenIndexDoesNotExist() {
        // Given
        var personId = 1;
        var documentId = 1;

        var contribution = new PersonDocumentContribution();
        var document = new Software();

        when(personContributionService.findContributionForResearcherAndDocument(personId,
            documentId)).thenReturn(contribution);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // When
        documentPublicationService.unbindResearcherFromContribution(personId, documentId);

        // Then
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    void shouldReturnNonAffiliatedDocuments() {
        // Given
        var organisationUnitId = 1;
        var personId = 100;
        var pageable = PageRequest.of(0, 10);

        var nonAffiliatedDocumentIds = List.of(10, 20, 30);
        var documentPublicationIndexes = List.of(
            new DocumentPublicationIndex(),
            new DocumentPublicationIndex(),
            new DocumentPublicationIndex()
        );
        var expectedPage =
            new PageImpl<>(documentPublicationIndexes, pageable, documentPublicationIndexes.size());

        when(personContributionService.getIdsOfNonRelatedDocuments(organisationUnitId, personId))
            .thenReturn(nonAffiliatedDocumentIds);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdIn(
            nonAffiliatedDocumentIds, pageable))
            .thenReturn(expectedPage);

        // When
        var result =
            documentPublicationService.findNonAffiliatedDocuments(organisationUnitId, personId,
                pageable);

        // Then
        assertEquals(expectedPage, result);
        verify(personContributionService, times(1)).getIdsOfNonRelatedDocuments(organisationUnitId,
            personId);
        verify(documentPublicationIndexRepository,
            times(1)).findDocumentPublicationIndexByDatabaseIdIn(nonAffiliatedDocumentIds,
            pageable);
    }

    @Test
    void shouldReturnEmptyPageWhenNoDocumentsFound() {
        // Given
        var organisationUnitId = 1;
        var personId = 100;
        var pageable = PageRequest.of(0, 10);

        when(personContributionService.getIdsOfNonRelatedDocuments(organisationUnitId, personId))
            .thenReturn(List.of());
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseIdIn(List.of(),
                pageable))
            .thenReturn(Page.empty());

        // When
        var result =
            documentPublicationService.findNonAffiliatedDocuments(organisationUnitId, personId,
                pageable);

        // Then
        assertTrue(result.isEmpty());
        verify(personContributionService, times(1)).getIdsOfNonRelatedDocuments(organisationUnitId,
            personId);
        verify(documentPublicationIndexRepository,
            times(1)).findDocumentPublicationIndexByDatabaseIdIn(List.of(), pageable);
    }

    @Test
    void shouldReturnFalseWhenNeitherDoiNorScopusIdExists() {
        // given
        var identifier = "10.1234/example-doi";
        var documentPublicationId = 1;
        when(documentRepository.existsByDoi(identifier, documentPublicationId)).thenReturn(false);
        when(documentRepository.existsByScopusId(identifier, documentPublicationId)).thenReturn(
            false);

        // when
        var result =
            documentPublicationService.isIdentifierInUse(identifier, documentPublicationId);

        // then
        assertFalse(result);
        verify(documentRepository).existsByDoi(identifier, documentPublicationId);
        verify(documentRepository).existsByScopusId(identifier, documentPublicationId);
    }

    @Test
    void shouldReturnTrueWhenIdentifierExists() {
        // given
        var identifier = "10.1234/example-doi";
        var documentPublicationId = 1;
        when(documentRepository.existsByDoi(identifier, documentPublicationId)).thenReturn(true);
        when(documentRepository.existsByScopusId(identifier, documentPublicationId)).thenReturn(
            false);

        // when
        var result =
            documentPublicationService.isIdentifierInUse(identifier, documentPublicationId);

        // then
        assertTrue(result);
        verify(documentRepository, atMostOnce()).existsByDoi(identifier, documentPublicationId);
        verify(documentRepository, atMostOnce()).existsByScopusId(identifier,
            documentPublicationId);
    }

    @Test
    void shouldReturnTrueWhenScopusIdExists() {
        // given
        var identifier = "SCOPUS123456";
        var documentPublicationId = 1;
        when(documentRepository.existsByDoi(identifier, documentPublicationId)).thenReturn(false);
        when(documentRepository.existsByScopusId(identifier, documentPublicationId)).thenReturn(
            true);

        // when
        var result =
            documentPublicationService.isIdentifierInUse(identifier, documentPublicationId);

        // then
        assertTrue(result);
        verify(documentRepository, atMostOnce()).existsByDoi(identifier, documentPublicationId);
        verify(documentRepository, atMostOnce()).existsByScopusId(identifier,
            documentPublicationId);
    }

    @Test
    void shouldReturnTrueWhenBothDoiAndScopusIdExist() {
        // given
        var identifier = "10.1234/example-doi";
        var documentPublicationId = 1;
        when(documentRepository.existsByDoi(identifier, documentPublicationId)).thenReturn(true);
        when(documentRepository.existsByScopusId(identifier, documentPublicationId)).thenReturn(
            true);

        // when
        var result =
            documentPublicationService.isIdentifierInUse(identifier, documentPublicationId);

        // then
        assertTrue(result);
        verify(documentRepository, atMostOnce()).existsByDoi(identifier, documentPublicationId);
        verify(documentRepository, atMostOnce()).existsByScopusId(identifier,
            documentPublicationId);
    }

    @Test
    void shouldUpdateAssessedByWhenReindexDocumentVolatileInformation() {
        // Given
        var documentId = 1;
        var documentIndex = mock(DocumentPublicationIndex.class);
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(documentIndex));
        var commissions = List.of(1, 2);
        when(commissionRepository.findCommissionsThatAssessedDocument(documentId)).thenReturn(
            commissions);

        // When
        documentPublicationService.reindexDocumentVolatileInformation(documentId);

        // Then
        verify(documentIndex).setAssessedBy(commissions);
    }

    @Test
    void shouldReturnResearchOutputIdsWhenDocumentExists() {
        // given
        var documentId = 1;
        var expectedResearchOutputIds = List.of(101, 102, 103);
        var documentIndex = new DocumentPublicationIndex();
        documentIndex.setResearchOutputIds(expectedResearchOutputIds);

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(documentIndex));

        // when
        var result = documentPublicationService.getResearchOutputIdsForDocument(documentId);

        // then
        assertEquals(expectedResearchOutputIds, result);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFetchingOutputsAndDocumentDoesNotExist() {
        // given
        var documentId = 999;
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // when / then
        var exception = assertThrows(NotFoundException.class,
            () -> documentPublicationService.getResearchOutputIdsForDocument(documentId));

        assertEquals("Document with ID " + documentId + " does not exist.", exception.getMessage());
    }

    @Test
    void shouldReturnDocumentCountsBelongingToInstitution() {
        // given
        var institutionId = 1;
        when(documentPublicationIndexRepository.countAssessable()).thenReturn(200L);
        when(documentPublicationIndexRepository.countAssessableByOrganisationUnitIds(institutionId))
            .thenReturn(80L);

        // when
        var result =
            documentPublicationService.getDocumentCountsBelongingToInstitution(institutionId);

        // then
        assertEquals(200L, result.a);
        assertEquals(80L, result.b);
    }

    @Test
    void shouldReturnAssessedDocumentCountsForCommission() {
        // given
        var institutionId = 1;
        var commissionId = 2;
        when(documentPublicationIndexRepository.countByAssessedBy(commissionId)).thenReturn(60L);
        when(documentPublicationIndexRepository.countByOrganisationUnitIdsAndAssessedBy(
            institutionId, commissionId))
            .thenReturn(25L);

        // when
        var result =
            documentPublicationService.getAssessedDocumentCountsForCommission(institutionId,
                commissionId);

        // then
        assertEquals(60L, result.a);
        assertEquals(25L, result.b);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnSearchFields(Boolean onlyExportFields) {
        // Given
        var expectedFields = List.of(
            new Triple<>("field1", List.of(new MultilingualContentDTO()), "Type1"),
            new Triple<>("field2", List.of(new MultilingualContentDTO()), "Type2")
        );

        when(searchFieldsLoader.getSearchFields(any(), anyBoolean())).thenReturn(expectedFields);

        // When
        var result = documentPublicationService.getSearchFields(onlyExportFields);

        // Then
        assertNotNull(result);
        assertEquals(expectedFields.size(), result.size());
    }

    @Test
    public void shouldReturnSortedWordFrequenciesForDocumentInSerbian() {
        // given
        Integer documentId = 123;
        DocumentPublicationIndex mockDoc = mock(DocumentPublicationIndex.class);
        List<String> terms = List.of("abc", "def", "abc", "xyz", "xyz", "xyz");

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(mockDoc));
        when(mockDoc.getWordcloudTokensSr()).thenReturn(terms);

        // when
        var result = documentPublicationService.getWordCloudForSingleDocument(documentId, false);

        // then
        assertEquals(3, result.size());
        assertEquals("xyz", result.get(0).a);
        assertEquals(3L, result.get(0).b);
        assertEquals("abc", result.get(1).a);
        assertEquals(2L, result.get(1).b);
        assertEquals("def", result.get(2).a);
        assertEquals(1L, result.get(2).b);
    }

    @Test
    public void shouldThrowNotFoundExceptionForMissingDocument() {
        // given
        Integer documentId = 999;
        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class, () -> {
            documentPublicationService.getWordCloudForSingleDocument(documentId, false);
        });
    }

    @Test
    public void shouldUseForeignLanguageTokensWhenRequested() {
        // given
        Integer documentId = 456;
        DocumentPublicationIndex mockDoc = mock(DocumentPublicationIndex.class);
        List<String> foreignTerms = List.of("uno", "dos", "uno", "tres");

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(mockDoc));
        when(mockDoc.getWordcloudTokensOther()).thenReturn(foreignTerms);

        // when
        var result = documentPublicationService.getWordCloudForSingleDocument(documentId, true);

        // then
        assertEquals(3, result.size());
        assertEquals("uno", result.getFirst().a);
        assertEquals(2L, result.getFirst().b);
    }

    @Test
    void testFindDocumentByCommonIdentifier_WithValidIds() {
        // Given
        var doi = "https://doi.org/10.1234/test";
        var openAlexId = "https://openalex.org/W123456789";
        var mockDocument = new JournalPublication();
        Optional<Document> expected = Optional.of(mockDocument);

        when(documentRepository.findByOpenAlexIdOrDoiOrScopusId("W123456789", "10.1234/test",
            "1234567"))
            .thenReturn(expected);

        // When
        var result =
            documentPublicationService.findDocumentByCommonIdentifier(doi, openAlexId, "1234567");

        // Then
        assertTrue(result.isPresent());
        assertEquals(mockDocument, result.get());
        verify(documentRepository).findByOpenAlexIdOrDoiOrScopusId("W123456789", "10.1234/test",
            "1234567");
    }

    @Test
    void testFindDocumentByCommonIdentifier_WithNullValues() {
        // Given
        when(documentRepository.findByOpenAlexIdOrDoiOrScopusId("NOT_PRESENT", "NOT_PRESENT",
            "NOT_PRESENT"))
            .thenReturn(Optional.empty());

        // When
        var result = documentPublicationService.findDocumentByCommonIdentifier(null, null, null);

        // Then
        assertTrue(result.isEmpty());
        verify(documentRepository).findByOpenAlexIdOrDoiOrScopusId("NOT_PRESENT", "NOT_PRESENT",
            "NOT_PRESENT");
    }

    @Test
    void testFindDocumentByCommonIdentifier_WithBlankValues() {
        // Given
        when(documentRepository.findByOpenAlexIdOrDoiOrScopusId("NOT_PRESENT", "NOT_PRESENT",
            "NOT_PRESENT"))
            .thenReturn(Optional.empty());

        // When
        var result = documentPublicationService.findDocumentByCommonIdentifier(" ", " ", " ");

        // Then
        assertTrue(result.isEmpty());
        verify(documentRepository).findByOpenAlexIdOrDoiOrScopusId("NOT_PRESENT", "NOT_PRESENT",
            "NOT_PRESENT");
    }

    @Test
    void shouldReturnTrueWhenDoiExists() {
        // Given
        var doi = "10.1234/example";
        when(documentRepository.existsByDoi(doi, null)).thenReturn(true);

        // When
        boolean result = documentPublicationService.isDoiInUse(doi);

        // Then
        assertTrue(result);
        verify(documentRepository).existsByDoi(doi, null);
    }

    @Test
    void shouldReturnFalseWhenDoiDoesNotExist() {
        // Given
        var doi = "10.5678/unused";
        when(documentRepository.existsByDoi(doi, null)).thenReturn(false);

        // When
        boolean result = documentPublicationService.isDoiInUse(doi);

        // Then
        assertFalse(result);
        verify(documentRepository).existsByDoi(doi, null);
    }
}
