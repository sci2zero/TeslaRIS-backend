package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.importer.LoadingConfigurationRepository;
import rs.teslaris.importer.dto.LoadingConfigurationDTO;
import rs.teslaris.importer.model.configuration.LoadingConfiguration;
import rs.teslaris.importer.service.impl.LoadingConfigurationServiceImpl;

@SpringBootTest
public class LoadingConfigurationServiceTest {

    @Mock
    private LoadingConfigurationRepository loadingConfigurationRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private PersonService personService;

    @Mock
    private InvolvementService involvementService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LoadingConfigurationServiceImpl loadingConfigurationService;


    @Test
    public void shouldSaveNewLoadingConfigurationWhenNotExists() {
        // Given
        var institutionId = 1;
        var dto = new LoadingConfigurationDTO(true, false, false);
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.empty());
        var institution = new OrganisationUnit();
        when(organisationUnitService.findOne(institutionId)).thenReturn(institution);

        // When
        loadingConfigurationService.saveLoadingConfiguration(institutionId, dto);

        // Then
        ArgumentCaptor<LoadingConfiguration> captor =
            ArgumentCaptor.forClass(LoadingConfiguration.class);
        verify(loadingConfigurationRepository).save(captor.capture());
        var saved = captor.getValue();

        assertTrue(saved.getSmartLoadingByDefault());
        assertFalse(saved.getLoadedEntitiesAreUnmanaged());
        assertFalse(saved.getPriorityLoading());
        assertEquals(institution, saved.getInstitution());
    }

    @Test
    public void shouldUpdateExistingLoadingConfiguration() {
        // Given
        Integer institutionId = 1;
        var dto = new LoadingConfigurationDTO(false, true, true);
        var existing = new LoadingConfiguration();
        existing.setInstitution(new OrganisationUnit());

        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.of(existing));

        // When
        loadingConfigurationService.saveLoadingConfiguration(institutionId, dto);

        // Then
        verify(loadingConfigurationRepository).save(existing);
        assertFalse(existing.getSmartLoadingByDefault());
        assertTrue(existing.getLoadedEntitiesAreUnmanaged());
        assertTrue(existing.getPriorityLoading());
    }

    @Test
    public void shouldReturnConfigurationFromDirectInstitution() {
        // Given
        var userId = 1;
        var personId = 10;
        var institutionId = 100;
        var config = new LoadingConfiguration();
        config.setSmartLoadingByDefault(true);
        config.setLoadedEntitiesAreUnmanaged(false);

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(involvementService.getDirectEmploymentInstitutionIdsForPerson(personId))
            .thenReturn(List.of(institutionId));
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.of(config));

        // When
        var result = loadingConfigurationService.getLoadingConfigurationForResearcherUser(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSmartLoadingByDefault());
        assertFalse(result.getLoadedEntitiesAreUnmanaged());
    }

    @Test
    public void shouldReturnConfigurationFromSuperOrganisationUnit() {
        // Given
        var userId = 1;
        var personId = 10;
        var institutionId = 100;
        var superOuId = 200;
        var config = new LoadingConfiguration();
        config.setSmartLoadingByDefault(false);
        config.setLoadedEntitiesAreUnmanaged(true);

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(involvementService.getDirectEmploymentInstitutionIdsForPerson(personId))
            .thenReturn(List.of(institutionId));
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.empty());
        when(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId))
            .thenReturn(List.of(superOuId));
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(superOuId))
            .thenReturn(Optional.of(config));

        // When
        var result = loadingConfigurationService.getLoadingConfigurationForResearcherUser(userId);

        // Then
        assertNotNull(result);
        assertFalse(result.getSmartLoadingByDefault());
        assertTrue(result.getLoadedEntitiesAreUnmanaged());
    }

    @Test
    public void shouldReturnNullWhenNoConfigurationFound() {
        // Given
        var userId = 1;
        var personId = 10;
        var institutionId = 100;

        when(personService.getPersonIdForUserId(userId)).thenReturn(personId);
        when(involvementService.getDirectEmploymentInstitutionIdsForPerson(personId))
            .thenReturn(List.of(institutionId));
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.empty());
        when(organisationUnitService.getSuperOUsHierarchyRecursive(institutionId))
            .thenReturn(List.of());

        // When
        var result = loadingConfigurationService.getLoadingConfigurationForResearcherUser(userId);

        // Then
        assertTrue(result.getSmartLoadingByDefault());
        assertTrue(result.getLoadedEntitiesAreUnmanaged());
    }

    @Test
    public void shouldGetLoadingConfigurationForEmployeeUser() {
        // Given
        var userId = 123;
        var institutionId = 456;
        when(userService.getUserOrganisationUnitId(userId)).thenReturn(institutionId);

        var configuration = new LoadingConfiguration();
        configuration.setSmartLoadingByDefault(true);
        configuration.setLoadedEntitiesAreUnmanaged(false);
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.of(configuration));

        // When
        var result =
            loadingConfigurationService.getLoadingConfigurationForEmployeeUser(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSmartLoadingByDefault());
        assertFalse(result.getLoadedEntitiesAreUnmanaged());
    }

    @Test
    public void shouldGetLoadingConfigurationForAdminUser() {
        // Given
        var institutionId = 789;

        var configuration = new LoadingConfiguration();
        configuration.setSmartLoadingByDefault(false);
        configuration.setLoadedEntitiesAreUnmanaged(true);
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.of(configuration));

        // When
        var result =
            loadingConfigurationService.getLoadingConfigurationForAdminUser(institutionId);

        // Then
        assertNotNull(result);
        assertFalse(result.getSmartLoadingByDefault());
        assertTrue(result.getLoadedEntitiesAreUnmanaged());
    }

    @Test
    public void shouldReturnDefaultConfigurationIfNotFoundForAdminSpecifiedInstitution() {
        // Given
        var institutionId = 101;
        when(loadingConfigurationRepository.getLoadingConfigurationForInstitution(institutionId))
            .thenReturn(Optional.empty());

        // When
        var result =
            loadingConfigurationService.getLoadingConfigurationForAdminUser(institutionId);

        // Then
        assertTrue(result.getSmartLoadingByDefault());
        assertTrue(result.getLoadedEntitiesAreUnmanaged());
    }
}
