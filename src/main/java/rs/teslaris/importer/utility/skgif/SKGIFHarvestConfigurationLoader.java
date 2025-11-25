package rs.teslaris.importer.utility.skgif;

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
public class SKGIFHarvestConfigurationLoader {

    private static SKGIFHarvestConfiguration skgifHarvestConfiguration = null;

    private static String externalOverrideConfiguration;


    public SKGIFHarvestConfigurationLoader(
        @Value("${harvest.skgif.configuration}") String externalOverrideConfiguration) {
        SKGIFHarvestConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            skgifHarvestConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                SKGIFHarvestConfiguration.class,
                "src/main/resources/harvest/skgifHarvestConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload SKG-IF harvest configuration: " + e.getMessage());
        }
    }

    @Nullable
    public static Source getSourceConfigurationByName(String sourceName) {
        return skgifHarvestConfiguration.sources().stream()
            .filter(source -> source.sourceName.equals(sourceName)).findFirst().orElse(null);
    }

    public static boolean sourceExists(String sourceName) {
        return skgifHarvestConfiguration.sources().stream()
            .anyMatch(source -> source.sourceName.equals(sourceName));
    }

    public static List<String> getAllSourceNames() {
        return skgifHarvestConfiguration.sources.stream().map(source -> source.sourceName).toList();
    }

    private record SKGIFHarvestConfiguration(
        @JsonProperty(value = "sources", required = true) List<Source> sources
    ) {
    }

    public record Source(
        @JsonProperty(value = "sourceName", required = true) String sourceName,
        @JsonProperty(value = "baseUrl", required = true) String baseUrl,
        @JsonProperty(value = "metadataFormatParameter", required = true) String metadataFormatParameter,
        @JsonProperty(value = "dateFromFilterParam", required = true) String dateFromFilterParam,
        @JsonProperty(value = "dateToFilterParam", required = true) String dateToFilterParam,
        @JsonProperty(value = "additionalFilters", required = true) String additionalFilters,
        @JsonProperty(value = "converterClass", required = true) String converterClass,
        @JsonProperty(value = "sourceIdentifierPrefix", required = true) String sourceIdentifierPrefix
    ) {
    }
}
