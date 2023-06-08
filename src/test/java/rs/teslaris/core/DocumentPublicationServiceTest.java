package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.JournalService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;
import rs.teslaris.core.service.impl.DocumentPublicationServiceImpl;

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
    private PersonContributionService personContributionService;

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
        var result = documentPublicationService.findDocumentById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenDocumentDoesNotExist() {
        // given
        when(documentRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> documentPublicationService.findDocumentById(1));

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
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.findDocumentFileById(documentFileId)).thenReturn(documentFile);

        // When
        documentPublicationService.deleteDocumentFile(documentId, documentFileId, isProof);

        // Then
        verify(documentRepository, times(1)).save(document);
        verify(documentFileService, times(1)).deleteDocumentFile(documentFile.getServerFilename());
    }

    @Test
    public void shouldDeleteDocumentFileWithFileItem() {
        // Given
        var documentId = 1;
        var documentFileId = 1;
        var isProof = false;
        var document = new JournalPublication();
        document.setFileItems(new HashSet<>());
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.findDocumentFileById(documentFileId)).thenReturn(documentFile);

        // When
        documentPublicationService.deleteDocumentFile(documentId, documentFileId, isProof);

        // Then
        verify(documentRepository, times(1)).save(document);
        verify(documentFileService, times(1)).deleteDocumentFile(documentFile.getServerFilename());
    }

    @Test
    public void shouldAddDocumentFileWithProof() {
        // Given
        var documentId = 1;
        var documentFiles = new ArrayList<DocumentFileDTO>();
        documentFiles.add(new DocumentFileDTO());
        var isProof = true;
        var document = new JournalPublication();
        document.setProofs(new HashSet<>());
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class))).thenReturn(
            documentFile);

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
        document.setFileItems(new HashSet<>());
        var documentFile = new DocumentFile();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class))).thenReturn(
            documentFile);

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
    public void shouldCreateJournalPublication() {
        // Given
        var publicationDTO = new JournalPublicationDTO();
        var document = new JournalPublication();

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(documentRepository.save(any())).thenReturn(document);

        // When
        var result = documentPublicationService.createJournalPublication(publicationDTO);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(publicationDTO));
        verify(documentRepository).save(eq(document));
    }

    @Test
    public void shouldEditJournalPublication() {
        // Given
        var publicationId = 1;
        var publicationDTO = new JournalPublicationDTO();
        var publicationToUpdate = new JournalPublication();
        publicationToUpdate.setTitle(new HashSet<>());
        publicationToUpdate.setSubTitle(new HashSet<>());
        publicationToUpdate.setDescription(new HashSet<>());
        publicationToUpdate.setKeywords(new HashSet<>());
        publicationToUpdate.setContributors(new HashSet<>());
        publicationToUpdate.setUris(new HashSet<>());

        when(documentRepository.findById(publicationId)).thenReturn(
            Optional.of(publicationToUpdate));

        // When
        documentPublicationService.editJournalPublication(publicationId, publicationDTO);

        // Then
        verify(documentRepository).findById(eq(publicationId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(publicationToUpdate), eq(publicationDTO));
    }
}
