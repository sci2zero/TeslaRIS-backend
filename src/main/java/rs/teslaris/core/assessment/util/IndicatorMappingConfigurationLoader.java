package rs.teslaris.core.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

@Component
public class IndicatorMappingConfigurationLoader {

    private static IndicatorMappingConfiguration indicatorMappingConfiguration = null;

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            loadConfiguration();
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload indicator mapping configuration: " + e.getMessage());
        }
    }

    private static synchronized void loadConfiguration() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        indicatorMappingConfiguration = objectMapper.readValue(
            new FileInputStream(
                "src/main/resources/assessment/indicatorMappingConfiguration.json"),
            IndicatorMappingConfiguration.class
        );
    }

    public static List<String> getIndicatorNameForLoaderMethodName(String methodName) {
        return indicatorMappingConfiguration.mappings.getOrDefault(methodName, new ArrayList<>());
    }

    private record IndicatorMappingConfiguration(
        @JsonProperty(value = "mappings", required = true) Map<String, List<String>> mappings
    ) {
    }
}

