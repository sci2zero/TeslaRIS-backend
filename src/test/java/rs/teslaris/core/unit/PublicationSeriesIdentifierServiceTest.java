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
import rs.teslaris.core.dto.identifier.PublicationSeriesIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.model.identifier.PublicationSeriesIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.PublicationSeriesIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.PublicationSeriesIdentifierServiceImpl;
import rs.teslaris.core.service.impl.identifier.cruddelegate.PublicationSeriesIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class PublicationSeriesIdentifierServiceTest {

    @Mock
    private PublicationSeriesIdentifierRepository publicationSeriesIdentifierRepository;

    @Mock
    private PublicationSeriesIdentifierJPAServiceImpl publicationSeriesIdentifierJPAService;

    @Mock
    private PublicationSeriesService publicationSeriesService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private PublicationSeriesIdentifierServiceImpl publicationSeriesIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllPublicationSeriesIdentifiersForPublicationSeries(AccessLevel accessLevel) {
        // Given
        var publicationSeriesId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.EVENT)));

        var publicationSeriesIdentifier1 = new PublicationSeriesIdentifier();
        publicationSeriesIdentifier1.setIdentifier(identifier);

        var publicationSeriesIdentifier2 = new PublicationSeriesIdentifier();
        publicationSeriesIdentifier2.setIdentifier(identifier);

        when(
            publicationSeriesIdentifierRepository.findIdentifiersForPublicationSeriesAndIdentifierAccessLevel(
                publicationSeriesId,
                accessLevel)).thenReturn(
            List.of(publicationSeriesIdentifier1, publicationSeriesIdentifier2));

        // When
        var response = publicationSeriesIdentifierService.getIdentifiersForPublicationSeries(
            publicationSeriesId, accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(publicationSeriesIdentifierRepository)
            .findIdentifiersForPublicationSeriesAndIdentifierAccessLevel(publicationSeriesId,
                accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForPublicationSeries() {
        // Given
        var publicationSeriesId = 1;

        when(
            publicationSeriesIdentifierRepository.findIdentifiersForPublicationSeriesAndIdentifierAccessLevel(
                publicationSeriesId,
                AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response = publicationSeriesIdentifierService.getIdentifiersForPublicationSeries(
            publicationSeriesId, AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreatePublicationSeriesIdentifier() {
        // Given
        var publicationSeriesIdentifierDTO = new PublicationSeriesIdentifierDTO();
        publicationSeriesIdentifierDTO.setPublicationSeriesId(1);
        publicationSeriesIdentifierDTO.setIdentifierId(1);
        publicationSeriesIdentifierDTO.setValue("10.1234/test");

        var newPublicationSeriesIdentifier = new PublicationSeriesIdentifier();
        newPublicationSeriesIdentifier.setIdentifier(new Identifier());

        when(publicationSeriesService.findOne(1)).thenReturn(new Journal());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(publicationSeriesIdentifierJPAService.save(any(PublicationSeriesIdentifier.class)))
            .thenReturn(newPublicationSeriesIdentifier);

        // When
        var result = publicationSeriesIdentifierService.createPublicationSeriesIdentifier(
            publicationSeriesIdentifierDTO, 1);

        // Then
        assertNotNull(result);
        verify(publicationSeriesService).findOne(1);
        verify(publicationSeriesIdentifierJPAService).save(any(PublicationSeriesIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingPublicationSeriesIdentifierWithNonExistentPublicationSeries() {
        // Given
        var publicationSeriesIdentifierDTO = new PublicationSeriesIdentifierDTO();
        publicationSeriesIdentifierDTO.setPublicationSeriesId(99);
        publicationSeriesIdentifierDTO.setIdentifierId(1);

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(publicationSeriesService.findOne(99)).thenThrow(
            new NotFoundException("PublicationSeries not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            publicationSeriesIdentifierService.createPublicationSeriesIdentifier(
                publicationSeriesIdentifierDTO, 1));

        verify(publicationSeriesIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdatePublicationSeriesIdentifier() {
        // Given
        var publicationSeriesIdentifierId = 1;
        var publicationSeriesIdentifierDTO = new PublicationSeriesIdentifierDTO();
        publicationSeriesIdentifierDTO.setPublicationSeriesId(1);
        publicationSeriesIdentifierDTO.setIdentifierId(1);
        publicationSeriesIdentifierDTO.setValue("10.1234/updated");

        var existingPublicationSeriesIdentifier = new PublicationSeriesIdentifier();
        existingPublicationSeriesIdentifier.setIdentifier(new Identifier());

        when(publicationSeriesIdentifierJPAService.findOne(publicationSeriesIdentifierId))
            .thenReturn(existingPublicationSeriesIdentifier);
        when(publicationSeriesService.findOne(1)).thenReturn(new BookSeries());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        publicationSeriesIdentifierService.updatePublicationSeriesIdentifier(
            publicationSeriesIdentifierId, publicationSeriesIdentifierDTO);

        // Then
        verify(publicationSeriesIdentifierJPAService).findOne(publicationSeriesIdentifierId);
        verify(publicationSeriesService).findOne(1);
        verify(publicationSeriesIdentifierJPAService).save(existingPublicationSeriesIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentPublicationSeriesIdentifier() {
        // Given
        var publicationSeriesIdentifierId = 99;
        var publicationSeriesIdentifierDTO = new PublicationSeriesIdentifierDTO();
        publicationSeriesIdentifierDTO.setPublicationSeriesId(1);
        publicationSeriesIdentifierDTO.setIdentifierId(1);

        when(publicationSeriesIdentifierJPAService.findOne(publicationSeriesIdentifierId))
            .thenThrow(new NotFoundException("PublicationSeries identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            publicationSeriesIdentifierService.updatePublicationSeriesIdentifier(
                publicationSeriesIdentifierId,
                publicationSeriesIdentifierDTO));

        verify(publicationSeriesIdentifierJPAService, never()).save(any());
    }
}
