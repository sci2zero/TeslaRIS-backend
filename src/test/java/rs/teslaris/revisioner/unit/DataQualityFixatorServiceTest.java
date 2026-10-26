package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.util.exceptionhandling.exception.FixatorPipelineAlreadyExistsException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.revisioner.dto.DataQualityFixatorPipelineDTO;
import rs.teslaris.revisioner.model.fixator.DataQualityFixatorPipeline;
import rs.teslaris.revisioner.repository.DataQualityFixatorPipelineRepository;
import rs.teslaris.revisioner.service.impl.DataQualityFixatorServiceImpl;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorConfigurationLoader;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorRegistry;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorConfigurationLoader.FixatorPipeline;

@SpringBootTest
public class DataQualityFixatorServiceTest {

    @Mock
    private DataQualityFixatorPipelineRepository dataQualityFixatorPipelineRepository;

    @Mock
    private DataQualityFixatorRegistry dataQualityFixatorRegistry;

    @InjectMocks
    private DataQualityFixatorServiceImpl dataQualityFixatorService;

    private DataQualityFixatorPipeline pipeline(String name, int version) {
        var pipeline = new DataQualityFixatorPipeline();
        pipeline.setPipelineName(name);
        pipeline.setVersion(version);
        pipeline.setMatchedRemarkKeys(List.of("titleTooLong"));
        pipeline.setAppliedFixators(List.of());
        pipeline.setCreateVersionOnEveryStep(false);

        return pipeline;
    }

    private FixatorPipeline configured(String name, int version) {
        return new FixatorPipeline(name, version, List.of("titleTooLong"), List.of(), true);
    }

    @Test
    public void shouldSeedPipelineThatIsNotPersistedYet() {
        var configuredPipeline = configured("titles", 3);

        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.empty());

        try (var loader = mockStatic(DataQualityFixatorConfigurationLoader.class)) {
            loader.when(DataQualityFixatorConfigurationLoader::listPipelines)
                .thenReturn(List.of(configuredPipeline));

            dataQualityFixatorService.synchroniseWithConfiguration();
        }

        var captor = ArgumentCaptor.forClass(DataQualityFixatorPipeline.class);
        verify(dataQualityFixatorPipelineRepository).save(captor.capture());

        assertEquals("titles", captor.getValue().getPipelineName());
        assertEquals(3, captor.getValue().getVersion());
        assertEquals(List.of("titleTooLong"), captor.getValue().getMatchedRemarkKeys());
    }

    @Test
    public void shouldOverwritePersistedPipelineOnlyWithGreaterConfiguredVersion() {
        var persistedPipeline = pipeline("titles", 4);

        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(persistedPipeline));

        try (var loader = mockStatic(DataQualityFixatorConfigurationLoader.class)) {
            loader.when(DataQualityFixatorConfigurationLoader::listPipelines)
                .thenReturn(List.of(configured("titles", 5)));

            dataQualityFixatorService.synchroniseWithConfiguration();
        }

        verify(dataQualityFixatorPipelineRepository).save(persistedPipeline);
        assertEquals(5, persistedPipeline.getVersion());
    }

    @Test
    public void shouldNotTouchPersistedPipelineWhenConfiguredVersionIsNotGreater() {
        var persistedPipeline = pipeline("titles", 5);

        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(persistedPipeline));

        try (var loader = mockStatic(DataQualityFixatorConfigurationLoader.class)) {
            loader.when(DataQualityFixatorConfigurationLoader::listPipelines)
                .thenReturn(List.of(configured("titles", 5)));

            dataQualityFixatorService.synchroniseWithConfiguration();
        }

        verify(dataQualityFixatorPipelineRepository, never()).save(any());
        assertEquals(5, persistedPipeline.getVersion());
    }

    @Test
    public void shouldSkipConfiguredPipelineWithoutName() {
        try (var loader = mockStatic(DataQualityFixatorConfigurationLoader.class)) {
            loader.when(DataQualityFixatorConfigurationLoader::listPipelines)
                .thenReturn(List.of(configured("", 1)));

            dataQualityFixatorService.synchroniseWithConfiguration();
        }

        verify(dataQualityFixatorPipelineRepository, never()).save(any());
    }

    @Test
    public void shouldBumpVersionOnUpdateAndRewriteConfigurationFile() {
        var persistedPipeline = pipeline("titles", 7);

        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(persistedPipeline));
        when(dataQualityFixatorPipelineRepository.save(any())).thenAnswer(
            invocation -> invocation.getArgument(0));
        when(dataQualityFixatorPipelineRepository.findAllByOrderByPipelineNameAsc()).thenReturn(
            List.of(persistedPipeline));

        DataQualityFixatorPipelineDTO result;

        try (var loader = mockStatic(DataQualityFixatorConfigurationLoader.class)) {
            result = dataQualityFixatorService.updatePipeline("titles",
                new DataQualityFixatorPipelineDTO("titles", 1, List.of("titleTooShort"), List.of(),
                    true));

            loader.verify(() -> DataQualityFixatorConfigurationLoader.persistConfiguration(any()));
        }

        assertEquals(8, result.version());
        assertEquals(List.of("titleTooShort"), result.matchedRemarkKeys());
    }

    @Test
    public void shouldStartNewPipelineAtVersionOne() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.empty());
        when(dataQualityFixatorPipelineRepository.save(any())).thenAnswer(
            invocation -> invocation.getArgument(0));
        when(dataQualityFixatorPipelineRepository.findAllByOrderByPipelineNameAsc()).thenReturn(
            List.of());

        DataQualityFixatorPipelineDTO result;

        try (var loader = mockStatic(DataQualityFixatorConfigurationLoader.class)) {
            result = dataQualityFixatorService.createPipeline(
                new DataQualityFixatorPipelineDTO("titles", 99, List.of(), List.of(), false));

            loader.verify(() -> DataQualityFixatorConfigurationLoader.persistConfiguration(any()));
        }

        assertEquals(1, result.version());
    }

    @Test
    public void shouldThrowWhenCreatingPipelineWithTakenName() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(pipeline("titles", 1)));

        assertThrows(FixatorPipelineAlreadyExistsException.class,
            () -> dataQualityFixatorService.createPipeline(
                new DataQualityFixatorPipelineDTO("titles", null, List.of(), List.of(), false)));
    }

    @Test
    public void shouldRejectPipelineReferencingUnknownFixator() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.empty());
        when(dataQualityFixatorRegistry.exists("ghostFixator")).thenReturn(false);

        assertThrows(NotFoundException.class,
            () -> dataQualityFixatorService.createPipeline(
                new DataQualityFixatorPipelineDTO("titles", null, List.of(),
                    List.of("ghostFixator"), false)));
    }

    @Test
    public void shouldThrowWhenDeletingUnknownPipeline() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.empty());

        assertThrows(NotFoundException.class,
            () -> dataQualityFixatorService.deletePipeline("titles"));
    }
}
