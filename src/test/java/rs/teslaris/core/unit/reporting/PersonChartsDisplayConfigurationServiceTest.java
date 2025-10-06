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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.reporting.PersonChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.PersonChartsDisplayConfiguration;
import rs.teslaris.reporting.service.impl.configuration.PersonChartsDisplayConfigurationServiceImpl;

@SpringBootTest
class PersonChartsDisplayConfigurationServiceTest {

    private static final Integer PERSON_ID = 1;
    private static final Integer INSTITUTION_ID = 10;

    @Mock
    private PersonChartsDisplayConfigurationRepository configurationRepository;

    @Mock
    private InvolvementService involvementService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private PersonChartsDisplayConfigurationServiceImpl service;


    private PersonChartsDisplayConfiguration configWithSettings() {
        var config = new PersonChartsDisplayConfiguration();
        var settings = new HashMap<String, ChartDisplaySettings>();
        settings.put("publicationCountTotal", new ChartDisplaySettings(true, true));
        settings.put("publicationCountByYear", new ChartDisplaySettings(false, false));
        config.setChartDisplaySettings(settings);
        return config;
    }

    @Test
    void shouldReturnWhenGivenInstitutionWithConfig() {
        // Given
        when(involvementService.getDirectEmploymentInstitutionIdsForPerson(PERSON_ID))
            .thenReturn(List.of(INSTITUTION_ID));

        var configuration = configWithSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(configuration));

        // When
        var result = service.getDisplaySettingsForPerson(PERSON_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.publicationCountTotal().getDisplay());
        assertTrue(result.publicationCountTotal().getSpanWholeRow());
        assertFalse(result.publicationCountByYear().getDisplay());
    }

    @Test
    void shouldFindInParentWhenGivenInstitutionWithoutDirectConfig() {
        // Given
        when(involvementService.getDirectEmploymentInstitutionIdsForPerson(PERSON_ID))
            .thenReturn(List.of(INSTITUTION_ID));

        // First call returns empty, second call (parent) returns config
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());
        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(new OrganisationUnitsRelation() {{
                setTargetOrganisationUnit(new OrganisationUnit() {{
                    setId(100);
                }});
            }});

        when(configurationRepository.getConfigurationForInstitution(100))
            .thenReturn(Optional.of(configWithSettings()));

        // When
        var result = service.getDisplaySettingsForPerson(PERSON_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.publicationCountTotal().getDisplay());
        assertTrue(result.publicationCountTotal().getSpanWholeRow());
    }

    @Test
    void shouldUpdateWhenGivenNoExistingConfig() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var dto = new PersonChartDisplaySettingsDTO(
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true)
        );

        // When
        service.savePersonDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository, times(1)).getConfigurationForInstitution(INSTITUTION_ID);
        verify(configurationRepository, times(1)).save(any(PersonChartsDisplayConfiguration.class));
    }

    @Test
    void shouldUpdateWhenGivenExistingConfig() {
        // Given
        var existingConfig = configWithSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(existingConfig));

        var dto = new PersonChartDisplaySettingsDTO(
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false),
            new ChartDisplaySettings(false, false)
        );

        // When
        service.savePersonDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository).save(argThat(config ->
            config.getChartDisplaySettings().containsKey("publicationCountTotal")
        ));
    }

    @Test
    void shouldAggregateTrueIfAnyTrueWhenGivenMultipleConfigurations() {
        // Given
        var config1 = new PersonChartsDisplayConfiguration();
        config1.setChartDisplaySettings(Map.of(
            "publicationCountTotal", new ChartDisplaySettings(false, false)
        ));

        var config2 = new PersonChartsDisplayConfiguration();
        config2.setChartDisplaySettings(Map.of(
            "publicationCountTotal", new ChartDisplaySettings(true, false)
        ));

        var configs = List.of(config1, config2);

        // When
        var result = invokePrivateCreateChartSetting(configs, "publicationCountTotal");

        // Then
        assertTrue(result.getDisplay());
        assertFalse(result.getSpanWholeRow());
    }

    private ChartDisplaySettings invokePrivateCreateChartSetting(
        List<PersonChartsDisplayConfiguration> configurations, String chartKey) {
        try {
            var method = PersonChartsDisplayConfigurationServiceImpl.class
                .getDeclaredMethod("createChartSetting", List.class, String.class);
            method.setAccessible(true);
            return (ChartDisplaySettings) method.invoke(service, configurations, chartKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

