package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.importer.dto.OrganisationUnitImportSourceConfigurationDTO;
import rs.teslaris.importer.model.configuration.OrganisationUnitImportSourceConfiguration;
import rs.teslaris.importer.repository.OrganisationUnitImportSourceConfigurationRepository;
import rs.teslaris.importer.service.impl.OrganisationUnitImportSourceConfigurationServiceImpl;

@SpringBootTest
class OrganisationUnitImportSourceConfigurationServiceTest {

    @Mock
    private OrganisationUnitImportSourceConfigurationRepository repository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private OrganisationUnitImportSourceConfigurationServiceImpl service;


    @Test
    void shouldReadConfiguration() {
        // Given
        var institutionId = 1;
        var entity = new OrganisationUnitImportSourceConfiguration();
        entity.setImportScopus(false);
        entity.setImportOpenAlex(true);
        entity.setImportWebOfScience(false);

        when(repository.findConfigurationForInstitution(institutionId))
            .thenReturn(Optional.of(entity));

        // When
        var result = service.readConfigurationForInstitution(institutionId);

        // Then
        assertNotNull(result);
        assertFalse(result.importScopus());
        assertTrue(result.importOpenAlex());
        assertFalse(result.importWebOfScience());
    }

    @Test
    void shouldReadDefaultConfigurationWhenNotFound() {
        // Given
        var institutionId = 1;
        when(repository.findConfigurationForInstitution(institutionId))
            .thenReturn(Optional.empty());

        // When
        var result = service.readConfigurationForInstitution(institutionId);

        // Then
        assertNotNull(result);
        assertTrue(result.importScopus());
        assertTrue(result.importOpenAlex());
        assertTrue(result.importWebOfScience());
    }

    @Test
    void shouldUpdateConfigurationForInstitution() {
        // Given
        var institutionId = 1;
        var existing = new OrganisationUnitImportSourceConfiguration();
        var dto = new OrganisationUnitImportSourceConfigurationDTO(false, false, true, true, true);

        when(repository.findConfigurationForInstitution(institutionId)).thenReturn(
            Optional.of(existing));

        // When
        service.saveConfigurationForInstitution(institutionId, dto);

        // Then
        assertFalse(existing.getImportScopus());
        assertFalse(existing.getImportOpenAlex());
        assertTrue(existing.getImportWebOfScience());

        verify(repository).save(existing);
    }

    @Test
    void shouldSaveConfigurationForInstitution() {
        // Given
        var institutionId = 1;
        var dto = new OrganisationUnitImportSourceConfigurationDTO(true, false, true, true, true);

        when(repository.findConfigurationForInstitution(institutionId))
            .thenReturn(Optional.empty());

        var captor = ArgumentCaptor.forClass(OrganisationUnitImportSourceConfiguration.class);

        // When
        service.saveConfigurationForInstitution(institutionId, dto);

        // Then
        verify(repository).save(captor.capture());
        OrganisationUnitImportSourceConfiguration saved = captor.getValue();

        assertTrue(saved.getImportScopus());
        assertFalse(saved.getImportOpenAlex());
        assertTrue(saved.getImportWebOfScience());
    }
}
