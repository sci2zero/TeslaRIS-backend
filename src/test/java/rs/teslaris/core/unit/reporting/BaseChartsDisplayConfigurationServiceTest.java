package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.impl.configuration.BaseChartsDisplayConfigurationServiceImpl;

@SpringBootTest
public class BaseChartsDisplayConfigurationServiceTest {

    private static final Integer ORGANISATION_UNIT_ID = 1;

    @Mock
    private ChartsDisplayConfigurationRepository configurationRepository;

    @InjectMocks
    private BaseChartsDisplayConfigurationServiceImpl service;


    @Test
    void shouldAggregateTrueIfAnyTrueWhenGivenMultipleConfigurations() {
        // Given
        var config1 = new ChartsDisplayConfiguration();
        config1.setPersonChartDisplaySettings(Map.of(
            "publicationCountTotal", new ChartDisplaySettings(false, false)
        ));

        var config2 = new ChartsDisplayConfiguration();
        config2.setPersonChartDisplaySettings(Map.of(
            "publicationCountTotal", new ChartDisplaySettings(true, false)
        ));

        var configs = List.of(config1, config2);

        // When
        var result = invokePrivateCreateChartSetting(configs, "publicationCountTotal");

        // Then
        assertTrue(result.getDisplay());
        assertFalse(result.getSpanWholeRow());
    }

    @Test
    void shouldReturnCompleteConfigurationWhenGivenExistingCompleteConfig() {
        // Given
        var existingConfig = createCompleteConfiguration();
        when(configurationRepository.getConfigurationForInstitution(ORGANISATION_UNIT_ID))
            .thenReturn(Optional.of(existingConfig));

        // When
        var result = service.getSavedConfigurationForOU(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);

        var personSettings = result.getPersonChartDisplaySettings();
        assertNotNull(personSettings);
        assertTrue(personSettings.getPublicationCountTotal().getDisplay());
        assertTrue(personSettings.getPublicationCountTotal().getSpanWholeRow());
        assertFalse(personSettings.getPublicationCountByYear().getDisplay());
        assertFalse(personSettings.getPublicationCountByYear().getSpanWholeRow());
        assertTrue(personSettings.getPublicationTypeByYear().getDisplay());
        assertFalse(personSettings.getPublicationTypeByYear().getSpanWholeRow());

        var ouSettings = result.getOuChartDisplaySettings();
        assertNotNull(ouSettings);
        assertFalse(ouSettings.getPublicationCountTotal().getDisplay());
        assertTrue(ouSettings.getPublicationCountTotal().getSpanWholeRow());
        assertTrue(ouSettings.getPublicationCountByYear().getDisplay());
        assertFalse(ouSettings.getPublicationCountByYear().getSpanWholeRow());
        assertTrue(ouSettings.getPublicationCountPersonLeaderboard().getDisplay());
        assertFalse(ouSettings.getPublicationCountPersonLeaderboard().getSpanWholeRow());
        assertFalse(ouSettings.getPublicationCountSubUnitLeaderboard().getDisplay());
        assertTrue(ouSettings.getPublicationCountSubUnitLeaderboard().getSpanWholeRow());

        var documentSettings = result.getDocumentChartDisplaySettings();
        assertNotNull(documentSettings);
        assertTrue(documentSettings.viewCountTotal().getDisplay());
        assertFalse(documentSettings.viewCountTotal().getSpanWholeRow());
        assertFalse(documentSettings.viewCountByMonth().getDisplay());
        assertTrue(documentSettings.viewCountByMonth().getSpanWholeRow());
        assertTrue(documentSettings.downloadCountTotal().getDisplay());
        assertTrue(documentSettings.downloadCountTotal().getSpanWholeRow());
    }

    @Test
    void shouldReturnDefaultConfigurationWhenGivenNoExistingConfig() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(ORGANISATION_UNIT_ID))
            .thenReturn(Optional.empty());

        // When
        var result = service.getSavedConfigurationForOU(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);

        var personSettings = result.getPersonChartDisplaySettings();
        assertNotNull(personSettings);
        assertTrue(personSettings.getPublicationCountTotal().getDisplay());
        assertFalse(personSettings.getPublicationCountTotal().getSpanWholeRow());
        assertTrue(personSettings.getPublicationCountByYear().getDisplay());
        assertFalse(personSettings.getPublicationCountByYear().getSpanWholeRow());
        assertTrue(personSettings.getPublicationTypeByYear().getDisplay());
        assertTrue(personSettings.getPublicationTypeByYear().getSpanWholeRow());

        var ouSettings = result.getOuChartDisplaySettings();
        assertNotNull(ouSettings);
        assertTrue(ouSettings.getPublicationCountTotal().getDisplay());
        assertFalse(ouSettings.getPublicationCountTotal().getSpanWholeRow());
        assertTrue(ouSettings.getPublicationCountPersonLeaderboard().getDisplay());
        assertTrue(ouSettings.getPublicationCountPersonLeaderboard().getSpanWholeRow());
        assertTrue(ouSettings.getCitationCountPersonLeaderboard().getDisplay());
        assertTrue(ouSettings.getCitationCountPersonLeaderboard().getSpanWholeRow());

        var documentSettings = result.getDocumentChartDisplaySettings();
        assertNotNull(documentSettings);
        assertTrue(documentSettings.viewCountTotal().getDisplay());
        assertFalse(documentSettings.viewCountTotal().getSpanWholeRow());
        assertTrue(documentSettings.viewCountByMonth().getDisplay());
        assertFalse(documentSettings.viewCountByMonth().getSpanWholeRow());
        assertTrue(documentSettings.downloadCountTotal().getDisplay());
        assertFalse(documentSettings.downloadCountTotal().getSpanWholeRow());
        assertTrue(documentSettings.viewCountByCountry().getDisplay());
        assertTrue(documentSettings.viewCountByCountry().getSpanWholeRow());
    }

    @Test
    void shouldHandleEmptyConfigurationWithNoSettingsInitialized() {
        // Given
        var emptyConfig = createEmptyConfiguration();
        when(configurationRepository.getConfigurationForInstitution(ORGANISATION_UNIT_ID))
            .thenReturn(Optional.of(emptyConfig));

        // When
        var result = service.getSavedConfigurationForOU(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);

        var personSettings = result.getPersonChartDisplaySettings();
        assertNotNull(personSettings);

        var ouSettings = result.getOuChartDisplaySettings();
        assertNotNull(ouSettings);

        var documentSettings = result.getDocumentChartDisplaySettings();
        assertNotNull(documentSettings);
    }

    @Test
    void shouldReturnAllThreeConfigurationTypes() {
        // Given
        var existingConfig = createCompleteConfiguration();
        when(configurationRepository.getConfigurationForInstitution(ORGANISATION_UNIT_ID))
            .thenReturn(Optional.of(existingConfig));

        // When
        var result = service.getSavedConfigurationForOU(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPersonChartDisplaySettings());
        assertNotNull(result.getOuChartDisplaySettings());
        assertNotNull(result.getDocumentChartDisplaySettings());
    }

    @Test
    void shouldApplyDefaultValuesConsistentlyAcrossAllConfigurationTypes() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(ORGANISATION_UNIT_ID))
            .thenReturn(Optional.empty());

        // When
        var result = service.getSavedConfigurationForOU(ORGANISATION_UNIT_ID);

        // Then
        assertNotNull(result);

        var personSettings = result.getPersonChartDisplaySettings();
        var ouSettings = result.getOuChartDisplaySettings();
        var documentSettings = result.getDocumentChartDisplaySettings();

        assertNotNull(personSettings.getPublicationCountTotal());
        assertNotNull(ouSettings.getPublicationCountTotal());
        assertNotNull(documentSettings.viewCountTotal());

        assertTrue(personSettings.getPublicationCountTotal().getDisplay());
        assertFalse(personSettings.getPublicationCountTotal().getSpanWholeRow());

        assertTrue(ouSettings.getPublicationCountTotal().getDisplay());
        assertFalse(ouSettings.getPublicationCountTotal().getSpanWholeRow());

        assertTrue(documentSettings.viewCountTotal().getDisplay());
        assertFalse(documentSettings.viewCountTotal().getSpanWholeRow());
    }

    private ChartDisplaySettings invokePrivateCreateChartSetting(
        List<ChartsDisplayConfiguration> configurations, String chartKey) {
        try {
            var method = BaseChartsDisplayConfigurationServiceImpl.class
                .getDeclaredMethod("createChartSetting", List.class, String.class, Function.class);
            method.setAccessible(true);
            return (ChartDisplaySettings) method.invoke(service, configurations, chartKey,
                (Function<ChartsDisplayConfiguration, Map<String, ChartDisplaySettings>>) ChartsDisplayConfiguration::getPersonChartDisplaySettings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChartsDisplayConfiguration createCompleteConfiguration() {
        var config = new ChartsDisplayConfiguration();

        // Person settings
        var personSettings = new HashMap<String, ChartDisplaySettings>();
        personSettings.put("publicationCountTotal", new ChartDisplaySettings(true, true));
        personSettings.put("publicationCountByYear", new ChartDisplaySettings(false, false));
        personSettings.put("publicationTypeByYear", new ChartDisplaySettings(true, false));
        personSettings.put("publicationCategoryByYear", new ChartDisplaySettings(false, true));
        personSettings.put("publicationTypeRatio", new ChartDisplaySettings(true, true));
        personSettings.put("publicationCategoryRatio", new ChartDisplaySettings(false, false));
        personSettings.put("citationCountTotal", new ChartDisplaySettings(true, false));
        personSettings.put("citationCountByYear", new ChartDisplaySettings(false, true));
        personSettings.put("viewCountTotal", new ChartDisplaySettings(true, true));
        personSettings.put("viewCountByMonth", new ChartDisplaySettings(false, false));
        personSettings.put("viewCountByCountry", new ChartDisplaySettings(true, false));
        config.setPersonChartDisplaySettings(personSettings);

        // OU settings
        var ouSettings = new HashMap<String, ChartDisplaySettings>();
        ouSettings.put("publicationCountTotal", new ChartDisplaySettings(false, true));
        ouSettings.put("publicationCountByYear", new ChartDisplaySettings(true, false));
        ouSettings.put("publicationTypeByYear", new ChartDisplaySettings(false, true));
        ouSettings.put("publicationCategoryByYear", new ChartDisplaySettings(true, false));
        ouSettings.put("publicationTypeRatio", new ChartDisplaySettings(false, false));
        ouSettings.put("publicationCategoryRatio", new ChartDisplaySettings(true, true));
        ouSettings.put("citationCountTotal", new ChartDisplaySettings(false, true));
        ouSettings.put("citationCountByYear", new ChartDisplaySettings(true, false));
        ouSettings.put("viewCountTotal", new ChartDisplaySettings(false, false));
        ouSettings.put("viewCountByMonth", new ChartDisplaySettings(true, true));
        ouSettings.put("viewCountByCountry", new ChartDisplaySettings(false, true));
        ouSettings.put("publicationCountPersonLeaderboard", new ChartDisplaySettings(true, false));
        ouSettings.put("publicationCountSubUnitLeaderboard", new ChartDisplaySettings(false, true));
        ouSettings.put("citationCountPersonLeaderboard", new ChartDisplaySettings(true, true));
        ouSettings.put("citationCountSubUnitLeaderboard", new ChartDisplaySettings(false, false));
        ouSettings.put("assessmentPointCountPersonLeaderboard",
            new ChartDisplaySettings(true, false));
        ouSettings.put("assessmentPointCountSubUnitLeaderboard",
            new ChartDisplaySettings(false, true));
        config.setOuChartDisplaySettings(ouSettings);

        // Document settings
        var documentSettings = new HashMap<String, ChartDisplaySettings>();
        documentSettings.put("viewCountTotal", new ChartDisplaySettings(true, false));
        documentSettings.put("viewCountByMonth", new ChartDisplaySettings(false, true));
        documentSettings.put("downloadCountTotal", new ChartDisplaySettings(true, true));
        documentSettings.put("downloadCountByMonth", new ChartDisplaySettings(false, false));
        documentSettings.put("viewCountByCountry", new ChartDisplaySettings(true, false));
        documentSettings.put("downloadCountByCountry", new ChartDisplaySettings(false, true));
        config.setDocumentChartDisplaySettings(documentSettings);

        return config;
    }

    private ChartsDisplayConfiguration createEmptyConfiguration() {
        return new ChartsDisplayConfiguration();
    }
}
