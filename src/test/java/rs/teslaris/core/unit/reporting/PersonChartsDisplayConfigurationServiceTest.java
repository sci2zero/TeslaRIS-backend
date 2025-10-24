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
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.impl.configuration.PersonChartsDisplayConfigurationServiceImpl;

@SpringBootTest
class PersonChartsDisplayConfigurationServiceTest {

    private static final Integer PERSON_ID = 1;
    private static final Integer INSTITUTION_ID = 10;

    @Mock
    private ChartsDisplayConfigurationRepository configurationRepository;

    @Mock
    private InvolvementService involvementService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private PersonChartsDisplayConfigurationServiceImpl service;


    private ChartsDisplayConfiguration configWithSettings() {
        var config = new ChartsDisplayConfiguration();
        var settings = new HashMap<String, ChartDisplaySettings>();
        settings.put("publicationCountTotal", new ChartDisplaySettings(true, true));
        settings.put("publicationCountByYear", new ChartDisplaySettings(false, false));
        config.setPersonChartDisplaySettings(settings);
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
        assertTrue(result.getPublicationCountTotal().getDisplay());
        assertTrue(result.getPublicationCountTotal().getSpanWholeRow());
        assertFalse(result.getPublicationCountByYear().getDisplay());
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
        assertTrue(result.getPublicationCountTotal().getDisplay());
        assertTrue(result.getPublicationCountTotal().getSpanWholeRow());
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
        verify(configurationRepository, times(1)).save(any(ChartsDisplayConfiguration.class));
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
            config.getPersonChartDisplaySettings().containsKey("publicationCountTotal")
        ));
    }
}

