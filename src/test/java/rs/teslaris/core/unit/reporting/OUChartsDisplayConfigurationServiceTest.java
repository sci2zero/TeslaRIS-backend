package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.OUChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.impl.configuration.OUChartsDisplayConfigurationServiceImpl;

@SpringBootTest
class OUChartsDisplayConfigurationServiceTest {

    private static final Integer ORGANISATION_UNIT_ID = 1;
    private static final Integer INSTITUTION_ID = 10;
    private static final Integer PARENT_INSTITUTION_ID = 100;

    @Mock
    private ChartsDisplayConfigurationRepository configurationRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private OUChartsDisplayConfigurationServiceImpl service;


    private ChartsDisplayConfiguration configWithOUSettings() {
        var config = new ChartsDisplayConfiguration();
        var settings = new HashMap<String, ChartDisplaySettings>();

        // Base settings
        settings.put("publicationCountTotal", new ChartDisplaySettings(true, true));
        settings.put("publicationCountByYear", new ChartDisplaySettings(false, false));

        // OU-specific settings
        settings.put("publicationCountPersonLeaderboard", new ChartDisplaySettings(true, false));
        settings.put("publicationCountSubUnitLeaderboard", new ChartDisplaySettings(false, true));
        settings.put("citationCountPersonLeaderboard", new ChartDisplaySettings(true, true));
        settings.put("citationCountSubUnitLeaderboard", new ChartDisplaySettings(false, false));
        settings.put("assessmentPointCountPersonLeaderboard",
            new ChartDisplaySettings(true, false));
        settings.put("assessmentPointCountSubUnitLeaderboard",
            new ChartDisplaySettings(false, true));

        config.setOuChartDisplaySettings(settings);
        return config;
    }

    private ChartsDisplayConfiguration configWithPartialOUSettings() {
        var config = new ChartsDisplayConfiguration();
        var settings = new HashMap<String, ChartDisplaySettings>();

        settings.put("publicationCountTotal", new ChartDisplaySettings(false, true));
        settings.put("publicationCountByYear", new ChartDisplaySettings(true, false));

        settings.put("publicationCountPersonLeaderboard", new ChartDisplaySettings(true, true));
        settings.put("citationCountSubUnitLeaderboard", new ChartDisplaySettings(false, false));

        config.setOuChartDisplaySettings(settings);
        return config;
    }

    @Test
    void shouldReturnOUSettingsWhenGivenOrganisationUnitWithDirectConfig() {
        // Given
        when(organisationUnitService.getSuperOUsHierarchyRecursive(ORGANISATION_UNIT_ID))
            .thenReturn(List.of(INSTITUTION_ID));

        var configuration = configWithOUSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(configuration));

        // When
        var result = service.getDisplaySettingsForOrganisationUnit(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);

        assertTrue(result.getPublicationCountTotal().getDisplay());
        assertTrue(result.getPublicationCountTotal().getSpanWholeRow());
        assertFalse(result.getPublicationCountByYear().getDisplay());

        assertTrue(result.getPublicationCountPersonLeaderboard().getDisplay());
        assertFalse(result.getPublicationCountPersonLeaderboard().getSpanWholeRow());

        assertFalse(result.getPublicationCountSubUnitLeaderboard().getDisplay());
        assertTrue(result.getPublicationCountSubUnitLeaderboard().getSpanWholeRow());

        assertTrue(result.getCitationCountPersonLeaderboard().getDisplay());
        assertTrue(result.getCitationCountPersonLeaderboard().getSpanWholeRow());

        assertFalse(result.getCitationCountSubUnitLeaderboard().getDisplay());
        assertFalse(result.getCitationCountSubUnitLeaderboard().getSpanWholeRow());

        assertTrue(result.getAssessmentPointPersonLeaderboard().getDisplay());
        assertFalse(result.getAssessmentPointPersonLeaderboard().getSpanWholeRow());

        assertFalse(result.getAssessmentPointSubUnitLeaderboard().getDisplay());
        assertTrue(result.getAssessmentPointSubUnitLeaderboard().getSpanWholeRow());
    }

    @Test
    void shouldFindInParentWhenGivenOrganisationUnitWithoutDirectConfig() {
        // Given
        when(organisationUnitService.getSuperOUsHierarchyRecursive(ORGANISATION_UNIT_ID))
            .thenReturn(List.of(INSTITUTION_ID, PARENT_INSTITUTION_ID));

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());
        when(configurationRepository.getConfigurationForInstitution(PARENT_INSTITUTION_ID))
            .thenReturn(Optional.of(configWithOUSettings()));

        // When
        var result = service.getDisplaySettingsForOrganisationUnit(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.getPublicationCountTotal().getDisplay());
        assertTrue(result.getPublicationCountPersonLeaderboard().getDisplay());
    }

    @Test
    void shouldReturnDefaultSettingsWhenGivenOrganisationUnitWithNoConfigInHierarchy() {
        // Given
        when(organisationUnitService.getSuperOUsHierarchyRecursive(ORGANISATION_UNIT_ID))
            .thenReturn(List.of(INSTITUTION_ID, PARENT_INSTITUTION_ID));

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());
        when(configurationRepository.getConfigurationForInstitution(PARENT_INSTITUTION_ID))
            .thenReturn(Optional.empty());

        // When
        var result = service.getDisplaySettingsForOrganisationUnit(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPublicationCountTotal());
        assertNotNull(result.getPublicationCountPersonLeaderboard());
        assertNotNull(result.getCitationCountSubUnitLeaderboard());
    }

    @Test
    void shouldHandlePartialSettingsWhenGivenOrganisationUnitWithPartialConfig() {
        // Given
        when(organisationUnitService.getSuperOUsHierarchyRecursive(ORGANISATION_UNIT_ID))
            .thenReturn(List.of(INSTITUTION_ID));

        var configuration = configWithPartialOUSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(configuration));

        // When
        var result = service.getDisplaySettingsForOrganisationUnit(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);

        assertFalse(result.getPublicationCountTotal().getDisplay());
        assertTrue(result.getPublicationCountTotal().getSpanWholeRow());

        assertTrue(result.getPublicationCountPersonLeaderboard().getDisplay());
        assertTrue(result.getPublicationCountPersonLeaderboard().getSpanWholeRow());

        assertNotNull(result.getPublicationCountSubUnitLeaderboard());
        assertNotNull(result.getCitationCountPersonLeaderboard());
    }

    @Test
    void shouldCreateNewConfigWhenSavingWithNoExistingConfig() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var dto = createCompleteOUChartDisplaySettingsDTO();

        // When
        service.saveOrganisationUnitDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository, times(1)).getConfigurationForInstitution(INSTITUTION_ID);
        verify(configurationRepository, times(1)).save(any(ChartsDisplayConfiguration.class));
    }

    @Test
    void shouldUpdateExistingConfigWhenSavingWithExistingConfig() {
        // Given
        var existingConfig = configWithOUSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(existingConfig));

        var dto = createCompleteOUChartDisplaySettingsDTO();

        // When
        service.saveOrganisationUnitDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository).save(argThat(config ->
            config.getOuChartDisplaySettings().containsKey("publicationCountPersonLeaderboard") &&
                config.getOuChartDisplaySettings().containsKey("citationCountSubUnitLeaderboard") &&
                config.getOuChartDisplaySettings()
                    .containsKey("assessmentPointCountPersonLeaderboard")
        ));
    }

    @Test
    void shouldSaveAllOUSpecificSettings() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var dto = createCompleteOUChartDisplaySettingsDTO();

        // When
        service.saveOrganisationUnitDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository).save(argThat(config -> {
            var settings = config.getOuChartDisplaySettings();
            return settings != null &&
                settings.containsKey("publicationCountPersonLeaderboard") &&
                settings.containsKey("publicationCountSubUnitLeaderboard") &&
                settings.containsKey("citationCountPersonLeaderboard") &&
                settings.containsKey("citationCountSubUnitLeaderboard") &&
                settings.containsKey("assessmentPointCountPersonLeaderboard") &&
                settings.containsKey("assessmentPointCountSubUnitLeaderboard");
        }));
    }

    @Test
    void shouldHandleEmptyOrganisationUnitHierarchy() {
        // Given
        when(organisationUnitService.getSuperOUsHierarchyRecursive(ORGANISATION_UNIT_ID))
            .thenReturn(List.of());

        // When
        var result = service.getDisplaySettingsForOrganisationUnit(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPublicationCountTotal());
        assertNotNull(result.getPublicationCountPersonLeaderboard());
    }

    private OUChartDisplaySettingsDTO createCompleteOUChartDisplaySettingsDTO() {
        return new OUChartDisplaySettingsDTO(
            // Base settings
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(false, true),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(false, true),
            // OU-specific settings
            new ChartDisplaySettings(true, false),  // publicationCountPersonLeaderboard
            new ChartDisplaySettings(false, true),  // publicationCountSubUnitLeaderboard
            new ChartDisplaySettings(true, true),   // citationCountPersonLeaderboard
            new ChartDisplaySettings(false, false), // citationCountSubUnitLeaderboard
            new ChartDisplaySettings(true, false),  // assessmentPointCountPersonLeaderboard
            new ChartDisplaySettings(false, true)   // assessmentPointCountSubUnitLeaderboard
        );
    }
}
