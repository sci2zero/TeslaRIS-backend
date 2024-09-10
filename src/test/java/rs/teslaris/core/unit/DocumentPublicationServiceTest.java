package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.DocumentPublicationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
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

    @InjectMocks
    private DocumentPublicationServiceImpl documentPublicationService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(documentPublicationService, "documentApprovedByDefault", true);
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
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false))).thenReturn(
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
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class),
            eq(!isProof))).thenReturn(
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

    @Test
    public void shouldFindDocumentPublicationsWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // when
        var result =
            documentPublicationService.searchDocumentPublications(new ArrayList<>(tokens),
                pageable, SearchRequestType.SIMPLE);

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
                pageable, SearchRequestType.ADVANCED);

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
        when(documentPublicationIndexRepository.findByAuthorIds(anyInt(),
            any(Pageable.class))).thenReturn(expectedPage);

        // when
        var resultPage =
            documentPublicationService.findResearcherPublications(authorId, pageable);

        // then
        assertEquals(expectedPage, resultPage);
        verify(documentPublicationIndexRepository).findByAuthorIds(authorId, pageable);
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
            documentPublicationService.findDocumentDuplicates(titles, "DOI", "scopusId");

        // then
        assertEquals(result.getTotalElements(), 2L);
    }
}
