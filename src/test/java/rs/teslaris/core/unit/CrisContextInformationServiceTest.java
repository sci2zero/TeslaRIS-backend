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
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.model.commontypes.CrisContextInformation;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.repository.commontypes.CrisContextInformationRepository;
import rs.teslaris.core.service.impl.commontypes.CrisContextInformationServiceImpl;

@SpringBootTest
public class CrisContextInformationServiceTest {

    @Mock
    private CrisContextInformationRepository crisContextInformationRepository;

    @InjectMocks
    private CrisContextInformationServiceImpl service;


    private CrisContextInformation configuration(Integer id, boolean assessment,
                                                 boolean library, boolean repository) {
        var configuration = new CrisContextInformation(assessment, library, repository,
            ".*", ".*", ".*", ".*", License.CC0);
        configuration.setId(id);
        return configuration;
    }

    @Test
    public void shouldReturnExistingConfiguration() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(configuration(1, false, true, false)));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.toggleAssessmentModule());
        assertTrue(result.toggleDigitalLibrary());
        assertFalse(result.toggleDigitalRepository());
        verify(crisContextInformationRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldReturnDefaultConfigurationWhenNoneExists() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertTrue(result.toggleAssessmentModule());
        assertTrue(result.toggleDigitalLibrary());
        assertTrue(result.toggleDigitalRepository());
        verify(crisContextInformationRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldKeepOldestAndDeleteRedundantConfigurationsOnRead() {
        // Given
        var oldest = configuration(1, false, false, true);
        var second = configuration(2, true, true, true);
        var third = configuration(3, true, false, false);
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(oldest, second, third));

        // When
        var result = service.readConfigurationForSystem();

        // Then
        assertFalse(result.toggleAssessmentModule());
        assertFalse(result.toggleDigitalLibrary());
        assertTrue(result.toggleDigitalRepository());

        var deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(crisContextInformationRepository).deleteAll(deletedCaptor.capture());
        assertEquals(List.of(second, third), deletedCaptor.getValue());
    }

    @Test
    public void shouldUpdateExistingConfigurationOnSave() {
        // Given
        var existing = configuration(1, true, true, true);
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(existing));
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(false, true, false,
            ".*", ".*", ".*", ".*", License.CC0);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        assertSame(existing, savedCaptor.getValue());
        assertFalse(existing.getToggleAssessmentModule());
        assertTrue(existing.getToggleDigitalLibrary());
        assertFalse(existing.getToggleDigitalRepository());
        assertEquals(dto, result);
        verify(crisContextInformationRepository, never()).deleteAll(anyList());
    }

    @Test
    public void shouldCreateConfigurationOnSaveWhenNoneExists() {
        // Given
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(Collections.emptyList());
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(true, false, true,
            ".*", ".*", ".*", ".*", License.CC0);

        // When
        var result = service.saveConfiguration(dto);

        // Then
        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
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
        when(crisContextInformationRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(oldest, redundant));
        when(crisContextInformationRepository.save(any(CrisContextInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CrisContextInformationDTO(false, false, true,
            ".*", ".*", ".*", ".*", License.CC0);

        // When
        service.saveConfiguration(dto);

        // Then
        var deletedCaptor = ArgumentCaptor.forClass(List.class);
        verify(crisContextInformationRepository).deleteAll(deletedCaptor.capture());
        assertEquals(List.of(redundant), deletedCaptor.getValue());

        var savedCaptor = ArgumentCaptor.forClass(CrisContextInformation.class);
        verify(crisContextInformationRepository).save(savedCaptor.capture());
        assertSame(oldest, savedCaptor.getValue());
        assertFalse(oldest.getToggleAssessmentModule());
        assertFalse(oldest.getToggleDigitalLibrary());
        assertTrue(oldest.getToggleDigitalRepository());
    }
}
