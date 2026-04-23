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
import rs.teslaris.core.dto.identifier.OrganisationUnitIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.model.identifier.OrganisationUnitIdentifier;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.OrganisationUnitIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.OrganisationUnitIdentifierServiceImpl;
import rs.teslaris.core.service.impl.identifier.cruddelegate.OrganisationUnitIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class OrganisationUnitIdentifierServiceTest {

    @Mock
    private OrganisationUnitIdentifierRepository organisationUnitIdentifierRepository;

    @Mock
    private OrganisationUnitIdentifierJPAServiceImpl organisationUnitIdentifierJPAService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private OrganisationUnitIdentifierServiceImpl organisationUnitIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllOrganisationUnitIdentifiersForOrganisationUnit(AccessLevel accessLevel) {
        // Given
        var organisationUnitId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.EVENT)));

        var organisationUnitIdentifier1 = new OrganisationUnitIdentifier();
        organisationUnitIdentifier1.setIdentifier(identifier);

        var organisationUnitIdentifier2 = new OrganisationUnitIdentifier();
        organisationUnitIdentifier2.setIdentifier(identifier);

        when(
            organisationUnitIdentifierRepository.findIdentifiersForOrganisationUnitAndIdentifierAccessLevel(
                organisationUnitId,
                accessLevel)).thenReturn(
            List.of(organisationUnitIdentifier1, organisationUnitIdentifier2));

        // When
        var response =
            organisationUnitIdentifierService.getIdentifiersForOrganisationUnit(organisationUnitId,
                accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(organisationUnitIdentifierRepository)
            .findIdentifiersForOrganisationUnitAndIdentifierAccessLevel(organisationUnitId,
                accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForOrganisationUnit() {
        // Given
        var organisationUnitId = 1;

        when(
            organisationUnitIdentifierRepository.findIdentifiersForOrganisationUnitAndIdentifierAccessLevel(
                organisationUnitId,
                AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response =
            organisationUnitIdentifierService.getIdentifiersForOrganisationUnit(organisationUnitId,
                AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreateOrganisationUnitIdentifier() {
        // Given
        var organisationUnitIdentifierDTO = new OrganisationUnitIdentifierDTO();
        organisationUnitIdentifierDTO.setOrganisationUnitId(1);
        organisationUnitIdentifierDTO.setIdentifierId(1);
        organisationUnitIdentifierDTO.setValue("10.1234/test");

        var newOrganisationUnitIdentifier = new OrganisationUnitIdentifier();
        newOrganisationUnitIdentifier.setIdentifier(new Identifier());

        when(organisationUnitService.findOne(1)).thenReturn(new OrganisationUnit());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(organisationUnitIdentifierJPAService.save(any(OrganisationUnitIdentifier.class)))
            .thenReturn(newOrganisationUnitIdentifier);

        // When
        var result = organisationUnitIdentifierService.createOrganisationUnitIdentifier(
            organisationUnitIdentifierDTO, 1);

        // Then
        assertNotNull(result);
        verify(organisationUnitService).findOne(1);
        verify(organisationUnitIdentifierJPAService).save(any(OrganisationUnitIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingOrganisationUnitIdentifierWithNonExistentOrganisationUnit() {
        // Given
        var organisationUnitIdentifierDTO = new OrganisationUnitIdentifierDTO();
        organisationUnitIdentifierDTO.setOrganisationUnitId(99);
        organisationUnitIdentifierDTO.setIdentifierId(1);

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(organisationUnitService.findOne(99)).thenThrow(
            new NotFoundException("OrganisationUnit not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            organisationUnitIdentifierService.createOrganisationUnitIdentifier(
                organisationUnitIdentifierDTO, 1));

        verify(organisationUnitIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdateOrganisationUnitIdentifier() {
        // Given
        var organisationUnitIdentifierId = 1;
        var organisationUnitIdentifierDTO = new OrganisationUnitIdentifierDTO();
        organisationUnitIdentifierDTO.setOrganisationUnitId(1);
        organisationUnitIdentifierDTO.setIdentifierId(1);
        organisationUnitIdentifierDTO.setValue("10.1234/updated");

        var existingOrganisationUnitIdentifier = new OrganisationUnitIdentifier();
        existingOrganisationUnitIdentifier.setIdentifier(new Identifier());

        when(organisationUnitIdentifierJPAService.findOne(organisationUnitIdentifierId))
            .thenReturn(existingOrganisationUnitIdentifier);
        when(organisationUnitService.findOne(1)).thenReturn(new OrganisationUnit());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        organisationUnitIdentifierService.updateOrganisationUnitIdentifier(
            organisationUnitIdentifierId, organisationUnitIdentifierDTO);

        // Then
        verify(organisationUnitIdentifierJPAService).findOne(organisationUnitIdentifierId);
        verify(organisationUnitService).findOne(1);
        verify(organisationUnitIdentifierJPAService).save(existingOrganisationUnitIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentOrganisationUnitIdentifier() {
        // Given
        var organisationUnitIdentifierId = 99;
        var organisationUnitIdentifierDTO = new OrganisationUnitIdentifierDTO();
        organisationUnitIdentifierDTO.setOrganisationUnitId(1);
        organisationUnitIdentifierDTO.setIdentifierId(1);

        when(organisationUnitIdentifierJPAService.findOne(organisationUnitIdentifierId))
            .thenThrow(new NotFoundException("OrganisationUnit identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            organisationUnitIdentifierService.updateOrganisationUnitIdentifier(
                organisationUnitIdentifierId,
                organisationUnitIdentifierDTO));

        verify(organisationUnitIdentifierJPAService, never()).save(any());
    }
}
