package rs.teslaris.core.exporter.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

public class ExportHandlersConfigurationLoader {

    private static ExportHandlersConfiguration exportHandlersConfiguration = null;

    private static void loadConfiguration() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        exportHandlersConfiguration = objectMapper.readValue(
            new FileInputStream("src/main/resources/export/exportHandlersConfiguration.json"),
            ExportHandlersConfiguration.class
        );
    }

    public static Optional<Handler> getHandlerByIdentifier(String identifier) {
        if (Objects.isNull(exportHandlersConfiguration)) {
            try {
                loadConfiguration();
            } catch (IOException e) {
                throw new StorageException(e.getMessage());
            }
        }
        return exportHandlersConfiguration.handlers().stream()
            .filter(handler -> handler.identifier().equals(identifier))
            .findFirst();
    }

    public record ExportHandlersConfiguration(
        List<Handler> handlers
    ) {
    }

    public record Handler(
        @JsonProperty("identifier") String identifier,
        @JsonProperty("internalInstitutionId") String internalInstitutionId,
        @JsonProperty("handlerName") String handlerName,
        @JsonProperty("handlerDescription") String handlerDescription,
        @JsonProperty("sets") List<Set> sets,
        @JsonProperty("metadataFormats") List<String> metadataFormats
    ) {
    }

    public record Set(
        @JsonProperty("setSpec") String setSpec,
        @JsonProperty("setName") String setName
    ) {
    }
}
