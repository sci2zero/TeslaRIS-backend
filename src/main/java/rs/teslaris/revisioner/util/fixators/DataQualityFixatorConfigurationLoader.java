package rs.teslaris.revisioner.util.fixators;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;
import rs.teslaris.revisioner.util.ObjectMapperProvider;

@Component
@Slf4j
public class DataQualityFixatorConfigurationLoader {

    private static final String CONFIG_PATH =
        "src/main/resources/dataQualityFixator/dataQualityFixatorConfiguration.json";

    private static String externalOverrideConfiguration;

    private static DataQualityFixatorConfiguration configuration;


    @Autowired
    public DataQualityFixatorConfigurationLoader(
        @Value("${revisioner.fixators.configuration}") String externalOverrideConfiguration) {
        DataQualityFixatorConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            configuration = ConfigurationLoaderUtil.loadConfiguration(
                DataQualityFixatorConfiguration.class,
                CONFIG_PATH,
                externalOverrideConfiguration);

            log.info("Loaded {} data quality fixator pipeline(s)", listPipelines().size());
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload data quality fixator configuration: " + e.getMessage());
        }
    }

    public static List<FixatorPipeline> listPipelines() {
        if (Objects.isNull(configuration) || Objects.isNull(configuration.pipelines())) {
            return Collections.emptyList();
        }

        return configuration.pipelines();
    }

    @Nullable
    public static FixatorPipeline getPipeline(String pipelineName) {
        return listPipelines().stream()
            .filter(pipeline -> pipelineName.equalsIgnoreCase(pipeline.pipelineName()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Rewrites the configuration file from the given pipelines and reloads it, writing to the
     * external override file whenever one is configured.
     */
    public static synchronized void persistConfiguration(List<FixatorPipeline> pipelines) {
        var targetPath = Path.of(
            Objects.isNull(externalOverrideConfiguration) ||
                externalOverrideConfiguration.isBlank()
                ? CONFIG_PATH
                : externalOverrideConfiguration);

        try {
            Files.createDirectories(targetPath.toAbsolutePath().getParent());

            ObjectMapperProvider.provideObjectmapper()
                .writer()
                .with(SerializationFeature.INDENT_OUTPUT)
                .writeValue(targetPath.toFile(), new DataQualityFixatorConfiguration(pipelines));
        } catch (IOException e) {
            throw new StorageException(
                "Failed to write data quality fixator configuration to " + targetPath +
                    ". Reason: " +
                    e.getMessage());
        }

        reloadConfiguration();
    }

    public record DataQualityFixatorConfiguration(

        @JsonProperty(value = "pipelines", required = true)
        List<FixatorPipeline> pipelines

    ) {
    }

    public record FixatorPipeline(

        @JsonProperty(value = "pipelineName", required = true)
        String pipelineName,

        @JsonProperty(value = "profileName", required = true)
        String profileName,

        @JsonProperty(value = "version", required = true)
        Integer version,

        @JsonProperty(value = "matchedRemarkKeys", required = true)
        List<String> matchedRemarkKeys,

        @JsonProperty(value = "appliedFixators", required = true)
        List<String> appliedFixators,

        @JsonProperty(value = "createVersionOnEveryStep", required = true)
        boolean createVersionOnEveryStep

    ) {
    }
}
