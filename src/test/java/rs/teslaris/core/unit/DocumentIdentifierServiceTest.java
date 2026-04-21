package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.identifier.DocumentIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.identifier.DocumentIdentifier;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.DocumentIdentifierRepository;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.DocumentIdentifierServiceImpl;
import rs.teslaris.core.service.impl.identifier.cruddelegate.DocumentIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class DocumentIdentifierServiceTest {

    @Mock
    private DocumentIdentifierRepository documentIdentifierRepository;

    @Mock
    private DocumentIdentifierJPAServiceImpl documentIdentifierJPAService;

    @Mock
    private DocumentLookupService documentLookupService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private DocumentIdentifierServiceImpl documentIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllDocumentIdentifiersForDocument(AccessLevel accessLevel) {
        // Given
        var documentId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.EVENT)));

        var documentIdentifier1 = new DocumentIdentifier();
        documentIdentifier1.setIdentifier(identifier);

        var documentIdentifier2 = new DocumentIdentifier();
        documentIdentifier2.setIdentifier(identifier);

        when(documentIdentifierRepository.findIdentifiersForDocumentAndIdentifierAccessLevel(
            documentId,
            accessLevel)).thenReturn(List.of(documentIdentifier1, documentIdentifier2));

        // When
        var response = documentIdentifierService.getIdentifiersForDocument(documentId, accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(documentIdentifierRepository)
            .findIdentifiersForDocumentAndIdentifierAccessLevel(documentId, accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForDocument() {
        // Given
        var documentId = 1;

        when(documentIdentifierRepository.findIdentifiersForDocumentAndIdentifierAccessLevel(
            documentId,
            AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response =
            documentIdentifierService.getIdentifiersForDocument(documentId, AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreateDocumentIdentifier() {
        // Given
        var documentIdentifierDTO = new DocumentIdentifierDTO();
        documentIdentifierDTO.setDocumentId(1);
        documentIdentifierDTO.setIdentifierId(1);
        documentIdentifierDTO.setValue("10.1234/test");

        var newDocumentIdentifier = new DocumentIdentifier();
        newDocumentIdentifier.setIdentifier(new Identifier());

        when(documentLookupService.fastDocumentLookup(1)).thenReturn(new JournalPublication());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(documentIdentifierJPAService.save(any(DocumentIdentifier.class)))
            .thenReturn(newDocumentIdentifier);

        // When
        var result = documentIdentifierService.createDocumentIdentifier(documentIdentifierDTO, 1);

        // Then
        assertNotNull(result);
        verify(documentLookupService).fastDocumentLookup(1);
        verify(documentIdentifierJPAService).save(any(DocumentIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDocumentIdentifierWithNonExistentDocument() {
        // Given
        var documentIdentifierDTO = new DocumentIdentifierDTO();
        documentIdentifierDTO.setDocumentId(99);
        documentIdentifierDTO.setIdentifierId(1);

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(documentLookupService.fastDocumentLookup(99)).thenThrow(
            new NotFoundException("Document not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            documentIdentifierService.createDocumentIdentifier(documentIdentifierDTO, 1));

        verify(documentIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdateDocumentIdentifier() {
        // Given
        var documentIdentifierId = 1;
        var documentIdentifierDTO = new DocumentIdentifierDTO();
        documentIdentifierDTO.setDocumentId(1);
        documentIdentifierDTO.setIdentifierId(1);
        documentIdentifierDTO.setValue("10.1234/updated");

        var existingDocumentIdentifier = new DocumentIdentifier();
        existingDocumentIdentifier.setIdentifier(new Identifier());

        when(documentIdentifierJPAService.findOne(documentIdentifierId))
            .thenReturn(existingDocumentIdentifier);
        when(documentLookupService.fastDocumentLookup(1)).thenReturn(new IntangibleProduct());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        documentIdentifierService.updateDocumentIdentifier(documentIdentifierId,
            documentIdentifierDTO);

        // Then
        verify(documentIdentifierJPAService).findOne(documentIdentifierId);
        verify(documentLookupService).fastDocumentLookup(1);
        verify(documentIdentifierJPAService).save(existingDocumentIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentDocumentIdentifier() {
        // Given
        var documentIdentifierId = 99;
        var documentIdentifierDTO = new DocumentIdentifierDTO();
        documentIdentifierDTO.setDocumentId(1);
        documentIdentifierDTO.setIdentifierId(1);

        when(documentIdentifierJPAService.findOne(documentIdentifierId))
            .thenThrow(new NotFoundException("Document identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            documentIdentifierService.updateDocumentIdentifier(documentIdentifierId,
                documentIdentifierDTO));

        verify(documentIdentifierJPAService, never()).save(any());
    }
}