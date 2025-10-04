package rs.teslaris.importer.utility.oaipmh;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;

@Component
public class OAIPMHHarvestConfigurationLoader {

    private static OAIPMHHarvestConfiguration oaipmhHarvestConfiguration = null;

    private static String externalOverrideConfiguration;


    public OAIPMHHarvestConfigurationLoader(
        @Value("${harvest.oaipmh.configuration}") String externalOverrideConfiguration) {
        OAIPMHHarvestConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            oaipmhHarvestConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                OAIPMHHarvestConfiguration.class,
                "src/main/resources/harvest/oaipmhHarvestConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload OAI-PMH harvest configuration: " + e.getMessage());
        }
    }

    @Nullable
    public static Source getSourceConfigurationByName(String sourceName) {
        return oaipmhHarvestConfiguration.sources().stream()
            .filter(source -> source.sourceName.equals(sourceName)).findFirst().orElse(null);
    }

    public static boolean sourceExists(String sourceName) {
        return oaipmhHarvestConfiguration.sources().stream()
            .anyMatch(source -> source.sourceName.equals(sourceName));
    }

    public static List<String> getAllSourceNames() {
        return oaipmhHarvestConfiguration.sources.stream().map(source -> source.sourceName)
            .toList();
    }

    private record OAIPMHHarvestConfiguration(
        @JsonProperty(value = "sources", required = true) List<Source> sources
    ) {
    }

    public record Source(
        @JsonProperty(value = "sourceName", required = true) String sourceName,
        @JsonProperty(value = "baseUrl", required = true) String baseUrl,
        @JsonProperty(value = "dataset", required = true) String dataset,
        @JsonProperty(value = "metadataFormat", required = true) String metadataFormat,
        @JsonProperty(value = "responseObjectClass", required = true) String responseObjectClass,
        @JsonProperty(value = "converterClass", required = true) String converterClass
    ) {
    }
}
