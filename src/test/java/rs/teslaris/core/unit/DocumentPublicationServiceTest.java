package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.DocumentPublicationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

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
        var expected = new JournalPublication();
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
        document.setProofs(new HashSet<>());
        document.setFileItems(new HashSet<>());
        document.setApproveStatus(ApproveStatus.REQUESTED);
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.findDocumentFileById(documentFileId)).thenReturn(documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.deleteDocumentFile(documentId, documentFileId, isProof);

        // Then
//        verify(documentRepository, times(1)).save(document);
//        verify(documentFileService, times(1)).deleteDocumentFile(documentFile.getServerFilename());
    }

    @Test
    public void shouldDeleteDocumentFileWithFileItem() {
        // Given
        var documentId = 1;
        var documentFileId = 1;
        var isProof = false;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        document.setFileItems(new HashSet<>());
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.findDocumentFileById(documentFileId)).thenReturn(documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.deleteDocumentFile(documentId, documentFileId, isProof);

        // Then
//        verify(documentRepository, times(1)).save(document);
//        verify(documentFileService, times(1)).deleteDocumentFile(documentFile.getServerFilename());
    }

    @Test
    public void shouldAddDocumentFileWithProof() {
        // Given
        var documentId = 1;
        var documentFiles = new ArrayList<DocumentFileDTO>();
        documentFiles.add(new DocumentFileDTO());
        var isProof = true;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        document.setProofs(new HashSet<>());
        document.setFileItems(new HashSet<>());
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false))).thenReturn(
            documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));

        // When
        documentPublicationService.addDocumentFile(documentId, documentFiles, isProof);

        // Then
        verify(documentRepository, times(1)).save(document);
    }

    @Test
    public void shouldAddDocumentFileWithFileItem() {
        // Given
        var documentId = 1;
        var documentFiles = new ArrayList<DocumentFileDTO>();
        documentFiles.add(new DocumentFileDTO());
        var isProof = false;
        var document = new JournalPublication();
        document.setApproveStatus(ApproveStatus.REQUESTED);
        document.setFileItems(new HashSet<>());
        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false))).thenReturn(
            documentFile);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            any())).thenReturn(Optional.of(new DocumentPublicationIndex()));
        when(documentFileService.findDocumentFileIndexByDatabaseId(any())).thenReturn(
            new DocumentFileIndex());

        // When
        documentPublicationService.addDocumentFile(documentId, documentFiles, isProof);

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
    public void shouldFindPersonsWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // when
        var result = documentPublicationService.searchDocumentPublicationsSimple(tokens, pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }
}
