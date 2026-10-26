package rs.teslaris.revisioner.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.util.exceptionhandling.exception.FixatorPipelineAlreadyExistsException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.revisioner.converter.DataQualityFixatorPipelineConverter;
import rs.teslaris.revisioner.dto.DataQualityFixatorPipelineDTO;
import rs.teslaris.revisioner.dto.FixatorSummaryDTO;
import rs.teslaris.revisioner.model.fixator.DataQualityFixatorPipeline;
import rs.teslaris.revisioner.repository.DataQualityFixatorPipelineRepository;
import rs.teslaris.revisioner.service.interfaces.DataQualityFixatorService;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorConfigurationLoader;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorConfigurationLoader.FixatorPipeline;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorRegistry;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityFixatorServiceImpl implements DataQualityFixatorService {

    private final DataQualityFixatorPipelineRepository dataQualityFixatorPipelineRepository;

    private final DataQualityFixatorRegistry dataQualityFixatorRegistry;


    @Override
    public List<FixatorSummaryDTO> listFixators() {
        return dataQualityFixatorRegistry.listFixators()
            .stream()
            .map(fixator -> new FixatorSummaryDTO(fixator.key(), fixator.name()))
            .toList();
    }

    @Override
    public List<DataQualityFixatorPipelineDTO> listPipelines() {
        return dataQualityFixatorPipelineRepository.findAllByOrderByPipelineNameAsc()
            .stream()
            .map(DataQualityFixatorPipelineConverter::toDTO)
            .toList();
    }

    @Override
    @Transactional
    public DataQualityFixatorPipelineDTO createPipeline(DataQualityFixatorPipelineDTO pipeline) {
        assertNameIsFree(pipeline.pipelineName());
        assertFixatorsExist(pipeline.appliedFixators());

        var newPipeline = new DataQualityFixatorPipeline();
        newPipeline.setVersion(1);
        apply(newPipeline, pipeline);

        var saved = dataQualityFixatorPipelineRepository.save(newPipeline);
        writeConfigurationFile();

        return DataQualityFixatorPipelineConverter.toDTO(saved);
    }

    @Override
    @Transactional
    public DataQualityFixatorPipelineDTO updatePipeline(String pipelineName,
                                                        DataQualityFixatorPipelineDTO pipeline) {
        var existingPipeline = findByName(pipelineName);

        if (!existingPipeline.getPipelineName().equals(pipeline.pipelineName())) {
            assertNameIsFree(pipeline.pipelineName());
        }

        assertFixatorsExist(pipeline.appliedFixators());

        existingPipeline.setVersion(existingPipeline.getVersion() + 1);
        apply(existingPipeline, pipeline);

        var saved = dataQualityFixatorPipelineRepository.save(existingPipeline);
        writeConfigurationFile();

        return DataQualityFixatorPipelineConverter.toDTO(saved);
    }

    @Override
    @Transactional
    public void deletePipeline(String pipelineName) {
        dataQualityFixatorPipelineRepository.delete(findByName(pipelineName));
        writeConfigurationFile();
    }

    /**
     * Seeds the configured pipelines, inserting the ones the database does not hold and overwriting
     * the ones whose configured version is strictly greater.
     */
    @Override
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void synchroniseWithConfiguration() {
        for (var configuredPipeline : DataQualityFixatorConfigurationLoader.listPipelines()) {
            if (Objects.isNull(configuredPipeline.pipelineName()) ||
                configuredPipeline.pipelineName().isBlank()) {
                continue;
            }

            var existingPipeline = dataQualityFixatorPipelineRepository
                .findByPipelineName(configuredPipeline.pipelineName());

            if (existingPipeline.isEmpty()) {
                dataQualityFixatorPipelineRepository.save(fromConfiguration(configuredPipeline));

                log.info("Seeded fixator pipeline '{}' version {}",
                    configuredPipeline.pipelineName(), configuredPipeline.version());
                continue;
            }

            var persistedPipeline = existingPipeline.get();

            if (configuredPipeline.version() <= persistedPipeline.getVersion()) {
                continue;
            }

            copyInto(persistedPipeline, configuredPipeline);
            dataQualityFixatorPipelineRepository.save(persistedPipeline);

            log.info("Updated fixator pipeline '{}' to version {} from configuration",
                configuredPipeline.pipelineName(), configuredPipeline.version());
        }
    }

    private void writeConfigurationFile() {
        DataQualityFixatorConfigurationLoader.persistConfiguration(
            dataQualityFixatorPipelineRepository.findAllByOrderByPipelineNameAsc()
                .stream()
                .map(DataQualityFixatorPipelineConverter::toConfigurationEntry)
                .toList());
    }

    private DataQualityFixatorPipeline findByName(String pipelineName) {
        return dataQualityFixatorPipelineRepository.findByPipelineName(pipelineName)
            .orElseThrow(() -> new NotFoundException(
                "Fixator pipeline '" + pipelineName + "' does not exist."));
    }

    private void assertNameIsFree(String pipelineName) {
        if (dataQualityFixatorPipelineRepository.findByPipelineName(pipelineName).isPresent()) {
            throw new FixatorPipelineAlreadyExistsException(
                "Fixator pipeline '" + pipelineName + "' already exists.");
        }
    }

    private void assertFixatorsExist(List<String> appliedFixators) {
        var unknownFixators = appliedFixators.stream()
            .filter(fixator -> !dataQualityFixatorRegistry.exists(fixator))
            .toList();

        if (!unknownFixators.isEmpty()) {
            throw new NotFoundException("Unknown fixator(s): " + String.join(", ", unknownFixators));
        }
    }

    private void apply(DataQualityFixatorPipeline pipeline, DataQualityFixatorPipelineDTO dto) {
        pipeline.setPipelineName(dto.pipelineName());
        pipeline.setMatchedRemarkKeys(new ArrayList<>(dto.matchedRemarkKeys()));
        pipeline.setAppliedFixators(new ArrayList<>(dto.appliedFixators()));
        pipeline.setCreateVersionOnEveryStep(dto.createVersionOnEveryStep());
    }

    private DataQualityFixatorPipeline fromConfiguration(FixatorPipeline configuredPipeline) {
        var pipeline = new DataQualityFixatorPipeline();
        copyInto(pipeline, configuredPipeline);

        return pipeline;
    }

    private void copyInto(DataQualityFixatorPipeline pipeline, FixatorPipeline configuredPipeline) {
        pipeline.setPipelineName(configuredPipeline.pipelineName());
        pipeline.setVersion(Objects.requireNonNullElse(configuredPipeline.version(), 1));
        pipeline.setMatchedRemarkKeys(
            new ArrayList<>(Objects.requireNonNullElse(configuredPipeline.matchedRemarkKeys(),
                List.of())));
        pipeline.setAppliedFixators(
            new ArrayList<>(Objects.requireNonNullElse(configuredPipeline.appliedFixators(),
                List.of())));
        pipeline.setCreateVersionOnEveryStep(configuredPipeline.createVersionOnEveryStep());
    }
}
