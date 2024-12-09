package rs.teslaris.core.unit.assessment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.service.impl.EntityIndicatorServiceImpl;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@SpringBootTest
public class EntityIndicatorServiceTest {

    @Mock
    private EntityIndicatorRepository entityIndicatorRepository;

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private EntityIndicatorServiceImpl entityIndicatorService;


    @Test
    public void shouldAddDocumentFileWithProof() {
        // Given
        var entityIndicatorId = 1;
        var entityIndicator = new DocumentIndicator();
        var documentFile = new DocumentFile();

        when(entityIndicatorRepository.findById(entityIndicatorId)).thenReturn(
            Optional.of(entityIndicator));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false))).thenReturn(
            documentFile);

        // When
        entityIndicatorService.addEntityIndicatorProof(new DocumentFileDTO(), entityIndicatorId);

        // Then
        verify(entityIndicatorRepository, times(1)).save(entityIndicator);
    }

    @Test
    public void shouldDeleteDocumentFileWithProof() {
        // Given
        var entityIndicatorId = 1;
        var documentFileId = 1;
        var entityIndicator = new DocumentIndicator();
        var documentFile = new DocumentFile();

        when(entityIndicatorRepository.findById(entityIndicatorId)).thenReturn(
            Optional.of(entityIndicator));
        when(documentFileService.findOne(documentFileId)).thenReturn(documentFile);

        // When
        entityIndicatorService.deleteEntityIndicatorProof(entityIndicatorId, documentFileId);

        // Then
        verify(documentFileService, times(1)).deleteDocumentFile(any());
    }

    @Test
    public void shouldDeleteEntityIndicator() {
        // Given
        var entityIndicatorId = 1;
        var entityIndicator = new DocumentIndicator();

        when(entityIndicatorRepository.findById(entityIndicatorId)).thenReturn(
            Optional.of(entityIndicator));

        // When
        entityIndicatorService.deleteEntityIndicator(entityIndicatorId);

        // Then
        verify(entityIndicatorRepository, times(1)).delete(entityIndicator);
    }
}
