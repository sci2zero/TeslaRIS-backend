package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import rs.teslaris.core.dto.commontypes.FeatureModuleTogglesDTO;
import rs.teslaris.core.model.commontypes.FeatureModuleToggles;
import rs.teslaris.core.repository.commontypes.FeatureModuleTogglesRepository;
import rs.teslaris.core.service.impl.commontypes.FeatureModuleTogglesServiceImpl;

@SpringBootTest
public class FeatureModuleTogglesServiceTest {

    @Mock
    private FeatureModuleTogglesRepository featureModuleTogglesRepository;

    @InjectMocks
    private FeatureModuleTogglesServiceImpl service;


    private FeatureModuleToggles configuration(Integer id, boolean assessment,
                                               boolean library, boolean repository) {
        var configuration = new FeatureModuleToggles(assessment, library, repository);
        configuration.setId(id);
        return configuration;
    }

    @Test
    public void shouldReturnExistingConfiguration() {
        // Given
        when(featureModuleTogglesRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configuration(1, false, true, false)));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.toggleAssessmentModule());
        assertTrue(result.toggleDigitalLibrary());
        assertFalse(result.toggleDigitalRepository());
        verify(featureModuleTogglesRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldReturnDefaultConfigurationWhenNoneExists() {
        // Given
        when(featureModuleTogglesRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertTrue(result.toggleAssessmentModule());
        assertTrue(result.toggleDigitalLibrary());
        assertTrue(result.toggleDigitalRepository());
        verify(featureModuleTogglesRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldKeepOldestAndDeleteRedundantConfigurationsOnRead() {
        // Given
        var oldest = configuration(1, false, false, true);
        var second = configuration(2, true, true, true);
        var third = configuration(3, true, false, false);
        when(featureModuleTogglesRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(oldest, second, third));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.toggleAssessmentModule());
        assertFalse(result.toggleDigitalLibrary());
        assertTrue(result.toggleDigitalRepository());

        var deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(featureModuleTogglesRepository).deleteAll(deletedCaptor.capture());
        assertEquals(List.of(second, third), deletedCaptor.getValue());
    }

    @Test
    public void shouldUpdateExistingConfigurationOnSave() {
        // Given
        var existing = configuration(1, true, true, true);
        when(featureModuleTogglesRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(existing));
        when(featureModuleTogglesRepository.save(any(FeatureModuleToggles.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new FeatureModuleTogglesDTO(false, true, false);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(FeatureModuleToggles.class);
        verify(featureModuleTogglesRepository).save(savedCaptor.capture());
        assertSame(existing, savedCaptor.getValue());
        assertFalse(existing.getToggleAssessmentModule());
        assertTrue(existing.getToggleDigitalLibrary());
        assertFalse(existing.getToggleDigitalRepository());
        assertEquals(dto, result);
        verify(featureModuleTogglesRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldCreateConfigurationOnSaveWhenNoneExists() {
        // Given
        when(featureModuleTogglesRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(featureModuleTogglesRepository.save(any(FeatureModuleToggles.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new FeatureModuleTogglesDTO(true, false, true);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(FeatureModuleToggles.class);
        verify(featureModuleTogglesRepository).save(savedCaptor.capture());
        var saved = savedCaptor.getValue();
        assertTrue(saved.getToggleAssessmentModule());
        assertFalse(saved.getToggleDigitalLibrary());
        assertTrue(saved.getToggleDigitalRepository());
        assertEquals(dto, result);
    }

    @Test
    public void shouldDeleteRedundantConfigurationsAndUpdateOldestOnSave() {
        // Given
        var oldest = configuration(1, true, true, true);
        var redundant = configuration(2, false, false, false);
        when(featureModuleTogglesRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(oldest, redundant));
        when(featureModuleTogglesRepository.save(any(FeatureModuleToggles.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new FeatureModuleTogglesDTO(false, false, true);

        // When
        service.saveConfiguration(dto);

        // Then
        var deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(featureModuleTogglesRepository).deleteAll(deletedCaptor.capture());
        assertEquals(List.of(redundant), deletedCaptor.getValue());

        var savedCaptor = ArgumentCaptor.forClass(FeatureModuleToggles.class);
        verify(featureModuleTogglesRepository).save(savedCaptor.capture());
        assertSame(oldest, savedCaptor.getValue());
        assertFalse(oldest.getToggleAssessmentModule());
        assertFalse(oldest.getToggleDigitalLibrary());
        assertTrue(oldest.getToggleDigitalRepository());
    }
}
