package rs.teslaris.exporter.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;

@Component
public class ExportHandlersConfigurationLoader {

    private static ExportHandlersConfiguration exportHandlersConfiguration = null;

    private static String externalOverrideConfiguration;


    public ExportHandlersConfigurationLoader(
        @Value("${export.handler.configuration}") String externalOverrideConfiguration) {
        ExportHandlersConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            exportHandlersConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                ExportHandlersConfiguration.class,
                "src/main/resources/export/exportHandlersConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload export handler configuration: " + e.getMessage());
        }
    }

    public static Optional<Handler> getHandlerByIdentifier(String identifier) {
        if (Objects.isNull(exportHandlersConfiguration)) {
            reloadConfiguration();
        }
        return exportHandlersConfiguration.handlers().stream()
            .filter(handler -> handler.identifier().equals(identifier))
            .findFirst();
    }

    public record ExportHandlersConfiguration(
        @JsonProperty(value = "handlers", required = true) List<Handler> handlers
    ) {
    }

    public record Handler(
        @JsonProperty(value = "identifier", required = true) String identifier,
        @JsonProperty(value = "internalInstitutionId", required = true) String internalInstitutionId,
        @JsonProperty(value = "handlerName", required = true) String handlerName,
        @JsonProperty(value = "handlerDescription", required = true) String handlerDescription,
        @JsonProperty(value = "handlerLanguage", required = true) String handlerLanguage,
        @JsonProperty(value = "supportLegacyIdentifiers", required = true) boolean supportLegacyIdentifiers,
        @JsonProperty(value = "dateFormat") String dateFormat,
        @JsonProperty(value = "supportedLanguages") List<String> supportedLanguages,
        @JsonProperty(value = "sets", required = true) List<Set> sets,
        @JsonProperty(value = "metadataFormats", required = true) List<String> metadataFormats,
        @JsonProperty(value = "exportOnlyActiveEmployees", required = true) Boolean exportOnlyActiveEmployees,
        @JsonProperty(value = "tokenExpirationTimeMinutes", required = true) Integer tokenExpirationTimeMinutes,
        @JsonProperty(value = "typeMappings", required = true) Map<String, String> typeMappings
    ) {
    }

    public record Set(
        @JsonProperty(value = "setSpec", required = true) String setSpec,
        @JsonProperty(value = "setName", required = true) String setName,
        @JsonProperty(value = "identifierSetSpec", required = true) String identifierSetSpec,
        @JsonProperty(value = "commonEntityClass", required = true) String commonEntityClass,
        @JsonProperty("publicationTypes") String publicationTypes,
        @JsonProperty(value = "concreteTypeFilters") String concreteTypeFilters,
        @JsonProperty("converterClass") String converterClass,
        @JsonProperty("additionalFilters") String additionalFilters,
        @JsonProperty(value = "default", defaultValue = "false") Boolean isDefaultSet
    ) {
    }
}
