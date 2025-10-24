package rs.teslaris.core.unit.reporting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.reporting.ChartsDisplayConfigurationRepository;
import rs.teslaris.reporting.dto.configuration.DocumentChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;
import rs.teslaris.reporting.model.ChartsDisplayConfiguration;
import rs.teslaris.reporting.service.impl.configuration.DocumentChartsDisplayConfigurationServiceImpl;

@SpringBootTest
class DocumentChartsDisplayConfigurationServiceTest {

    private static final Integer DOCUMENT_ID = 1;
    private static final Integer INSTITUTION_ID = 10;
    private static final Integer PARENT_INSTITUTION_ID = 100;
    private static final Integer INSTITUTION_ID_2 = 20;

    @Mock
    private ChartsDisplayConfigurationRepository configurationRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private DocumentChartsDisplayConfigurationServiceImpl service;


    private DocumentPublicationIndex createDocumentIndex(List<Integer> organisationUnitIds) {
        var documentIndex = new DocumentPublicationIndex();
        documentIndex.setOrganisationUnitIdsActive(organisationUnitIds);
        return documentIndex;
    }

    private ChartsDisplayConfiguration configWithDocumentSettings() {
        var config = new ChartsDisplayConfiguration();
        var settings = new HashMap<String, ChartDisplaySettings>();

        settings.put("viewCountTotal", new ChartDisplaySettings(true, true));
        settings.put("viewCountByMonth", new ChartDisplaySettings(false, false));
        settings.put("downloadCountTotal", new ChartDisplaySettings(true, false));
        settings.put("downloadCountByMonth", new ChartDisplaySettings(false, true));
        settings.put("viewCountByCountry", new ChartDisplaySettings(true, true));
        settings.put("downloadCountByCountry", new ChartDisplaySettings(false, false));

        config.setDocumentChartDisplaySettings(settings);
        return config;
    }

    private ChartsDisplayConfiguration configWithPartialDocumentSettings() {
        var config = new ChartsDisplayConfiguration();
        var settings = new HashMap<String, ChartDisplaySettings>();

        settings.put("viewCountTotal", new ChartDisplaySettings(false, true));
        settings.put("downloadCountTotal", new ChartDisplaySettings(true, false));
        settings.put("viewCountByCountry", new ChartDisplaySettings(true, true));

        config.setDocumentChartDisplaySettings(settings);
        return config;
    }

    @Test
    void shouldReturnDocumentSettingsWhenGivenDocumentWithDirectInstitutionConfig() {
        // Given
        var documentIndex = createDocumentIndex(List.of(INSTITUTION_ID));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(null); // No parent

        var configuration = configWithDocumentSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(configuration));

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.viewCountTotal().getDisplay());
        assertTrue(result.viewCountTotal().getSpanWholeRow());
        assertFalse(result.viewCountByMonth().getDisplay());
        assertTrue(result.downloadCountTotal().getDisplay());
        assertFalse(result.downloadCountTotal().getSpanWholeRow());
        assertFalse(result.downloadCountByMonth().getDisplay());
        assertTrue(result.downloadCountByMonth().getSpanWholeRow());
        assertTrue(result.viewCountByCountry().getDisplay());
        assertTrue(result.viewCountByCountry().getSpanWholeRow());
        assertFalse(result.downloadCountByCountry().getDisplay());
        assertFalse(result.downloadCountByCountry().getSpanWholeRow());
    }

    @Test
    void shouldFindInParentWhenGivenDocumentWithInstitutionWithoutDirectConfig() {
        // Given
        var documentIndex = createDocumentIndex(List.of(INSTITUTION_ID));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var parentRelation = new OrganisationUnitsRelation();
        parentRelation.setTargetOrganisationUnit(new OrganisationUnit() {{
            setId(PARENT_INSTITUTION_ID);
        }});
        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(parentRelation);

        when(configurationRepository.getConfigurationForInstitution(PARENT_INSTITUTION_ID))
            .thenReturn(Optional.of(configWithDocumentSettings()));

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.viewCountTotal().getDisplay());
        assertTrue(result.downloadCountTotal().getDisplay());
    }

    @Test
    void shouldUseFirstAvailableConfigWhenDocumentHasMultipleInstitutions() {
        // Given
        var documentIndex = createDocumentIndex(List.of(INSTITUTION_ID, INSTITUTION_ID_2));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());
        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(null);

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID_2))
            .thenReturn(Optional.of(configWithDocumentSettings()));

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.viewCountTotal().getDisplay());
        verify(configurationRepository).getConfigurationForInstitution(INSTITUTION_ID_2);
    }

    @Test
    void shouldReturnDefaultSettingsWhenGivenDocumentWithNoConfigInAnyInstitution() {
        // Given
        var documentIndex = createDocumentIndex(List.of(INSTITUTION_ID, INSTITUTION_ID_2));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());
        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(null);

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID_2))
            .thenReturn(Optional.empty());
        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID_2))
            .thenReturn(null);

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.viewCountTotal());
        assertNotNull(result.downloadCountTotal());
        assertNotNull(result.viewCountByCountry());
        assertNotNull(result.downloadCountByCountry());
    }

    @Test
    void shouldHandlePartialSettingsWhenGivenInstitutionWithPartialConfig() {
        // Given
        var documentIndex = createDocumentIndex(List.of(INSTITUTION_ID));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(null);

        var configuration = configWithPartialDocumentSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(configuration));

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);

        assertFalse(result.viewCountTotal().getDisplay());
        assertTrue(result.viewCountTotal().getSpanWholeRow());

        assertTrue(result.downloadCountTotal().getDisplay());
        assertFalse(result.downloadCountTotal().getSpanWholeRow());

        assertTrue(result.viewCountByCountry().getDisplay());
        assertTrue(result.viewCountByCountry().getSpanWholeRow());

        assertNotNull(result.viewCountByMonth());
        assertNotNull(result.downloadCountByMonth());
        assertNotNull(result.downloadCountByCountry());
    }

    @Test
    void shouldTraverseMultipleLevelsInHierarchy() {
        // Given
        var documentIndex = createDocumentIndex(List.of(INSTITUTION_ID));
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var level1Relation = new OrganisationUnitsRelation();
        level1Relation.setTargetOrganisationUnit(new OrganisationUnit() {{
            setId(101);
        }});
        when(organisationUnitService.getSuperOrganisationUnitRelation(INSTITUTION_ID))
            .thenReturn(level1Relation);

        when(configurationRepository.getConfigurationForInstitution(101))
            .thenReturn(Optional.empty());

        var level2Relation = new OrganisationUnitsRelation();
        level2Relation.setTargetOrganisationUnit(new OrganisationUnit() {{
            setId(PARENT_INSTITUTION_ID);
        }});
        when(organisationUnitService.getSuperOrganisationUnitRelation(101))
            .thenReturn(level2Relation);

        when(configurationRepository.getConfigurationForInstitution(PARENT_INSTITUTION_ID))
            .thenReturn(Optional.of(configWithDocumentSettings()));

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.viewCountTotal().getDisplay());
        verify(configurationRepository).getConfigurationForInstitution(PARENT_INSTITUTION_ID);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDocumentDoesNotExist() {
        // Given
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            service.getDisplaySettingsForDocument(DOCUMENT_ID);
        });
    }

    @Test
    void shouldCreateNewConfigWhenSavingDocumentSettingsWithNoExistingConfig() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var dto = createCompleteDocumentChartDisplaySettingsDTO();

        // When
        service.saveDocumentDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository, times(1)).getConfigurationForInstitution(INSTITUTION_ID);
        verify(configurationRepository, times(1)).save(any(ChartsDisplayConfiguration.class));
    }

    @Test
    void shouldUpdateExistingConfigWhenSavingDocumentSettingsWithExistingConfig() {
        // Given
        var existingConfig = configWithDocumentSettings();
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.of(existingConfig));

        var dto = createCompleteDocumentChartDisplaySettingsDTO();

        // When
        service.saveDocumentDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository).save(argThat(config ->
            config.getDocumentChartDisplaySettings().containsKey("viewCountTotal") &&
                config.getDocumentChartDisplaySettings().containsKey("downloadCountByCountry")
        ));
    }

    @Test
    void shouldSaveAllDocumentSpecificSettings() {
        // Given
        when(configurationRepository.getConfigurationForInstitution(INSTITUTION_ID))
            .thenReturn(Optional.empty());

        var dto = createCompleteDocumentChartDisplaySettingsDTO();

        // When
        service.saveDocumentDisplaySettings(INSTITUTION_ID, dto);

        // Then
        verify(configurationRepository).save(argThat(config -> {
            var settings = config.getDocumentChartDisplaySettings();
            return settings != null &&
                settings.containsKey("viewCountTotal") &&
                settings.containsKey("viewCountByMonth") &&
                settings.containsKey("downloadCountTotal") &&
                settings.containsKey("downloadCountByMonth") &&
                settings.containsKey("viewCountByCountry") &&
                settings.containsKey("downloadCountByCountry");
        }));
    }

    @Test
    void shouldHandleDocumentWithEmptyInstitutionList() {
        // Given
        var documentIndex = createDocumentIndex(List.of());
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            DOCUMENT_ID))
            .thenReturn(Optional.of(documentIndex));

        // When
        var result = service.getDisplaySettingsForDocument(DOCUMENT_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.viewCountTotal());
        assertNotNull(result.downloadCountTotal());
    }

    private DocumentChartDisplaySettingsDTO createCompleteDocumentChartDisplaySettingsDTO() {
        return new DocumentChartDisplaySettingsDTO(
            new ChartDisplaySettings(true, true),   // viewCountTotal
            new ChartDisplaySettings(false, false), // viewCountByMonth
            new ChartDisplaySettings(true, false),  // downloadCountTotal
            new ChartDisplaySettings(false, true),  // downloadCountByMonth
            new ChartDisplaySettings(true, true),   // viewCountByCountry
            new ChartDisplaySettings(false, false)  // downloadCountByCountry
        );
    }
}
