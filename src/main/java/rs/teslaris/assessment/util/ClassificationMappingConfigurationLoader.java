package rs.teslaris.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;

@Component
public class ClassificationMappingConfigurationLoader {

    private static ClassificationMappingConfigurationLoader.ClassificationMappingConfiguration
        classificationMappingConfiguration = null;

    private static String externalOverrideConfiguration;


    public ClassificationMappingConfigurationLoader(
        @Value("${assessment.classifications.mapping}") String externalOverrideConfiguration) {
        ClassificationMappingConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            classificationMappingConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                ClassificationMappingConfigurationLoader.ClassificationMappingConfiguration.class,
                "src/main/resources/assessment/classificationMappingConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload classification mapping configuration: " + e.getMessage());
        }
    }

    public static ClassificationMappingConfigurationLoader.ClassificationMapping fetchClassificationMappingConfiguration(
        String mappingName) {
        return classificationMappingConfiguration.mappings().getOrDefault(
            mappingName, null);
    }

    public static List<String> fetchAllConfigurationNames() {
        return classificationMappingConfiguration.mappings().keySet().stream().toList();
    }

    private record ClassificationMappingConfiguration(
        @JsonProperty(value = "mappings", required = true) Map<String, ClassificationMapping> mappings
    ) {
    }

    public record ClassificationMapping(
        @JsonProperty(value = "yearParseRegex", required = true) String yearParseRegex,
        @JsonProperty(value = "nameColumn", required = true) Integer nameColumn,
        @JsonProperty(value = "issnColumn", required = true) Integer issnColumn,
        @JsonProperty(value = "issnDelimiter", required = true) String issnDelimiter,
        @JsonProperty(value = "categoryColumn", required = true) Integer categoryColumn,
        @JsonProperty(value = "classificationColumn", required = true) Integer classificationColumn,
        @JsonProperty(value = "defaultLanguage", required = true) String defaultLanguage,
        @JsonProperty(value = "parallelize", required = true) Boolean parallelize,
        @JsonProperty(value = "discriminators")
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<Discriminator> discriminators
    ) {
    }

    public record Discriminator(
        @JsonProperty(value = "columnId", required = true) Integer columnId,
        @JsonProperty(value = "acceptedValues", required = true) List<String> acceptedValues
    ) {
    }
}
