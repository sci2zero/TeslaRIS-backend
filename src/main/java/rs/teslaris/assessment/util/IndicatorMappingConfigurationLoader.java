package rs.teslaris.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.util.ConfigurationLoaderUtil;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

@Component
public class IndicatorMappingConfigurationLoader {

    private static IndicatorMappingConfiguration indicatorMappingConfiguration = null;

    private static String externalOverrideConfiguration;


    public IndicatorMappingConfigurationLoader(
        @Value("${assessment.indicator.mapping}") String externalOverrideConfiguration) {
        IndicatorMappingConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            indicatorMappingConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                IndicatorMappingConfiguration.class,
                "src/main/resources/assessment/indicatorMappingConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload indicator mapping configuration: " + e.getMessage());
        }
    }

    public static List<String> getIndicatorNameForLoaderMethodName(String methodName) {
        return indicatorMappingConfiguration.mappings.getOrDefault(methodName, new ArrayList<>());
    }

    @Nullable
    public static String getLocaleOffsetForStatisticsPeriod(StatisticsType statisticsType,
                                                            String indicatorCode) {
        return switch (statisticsType) {
            case VIEW ->
                indicatorMappingConfiguration.offsets.views.getOrDefault(indicatorCode, null);
            case DOWNLOAD ->
                indicatorMappingConfiguration.offsets.downloads.getOrDefault(indicatorCode, null);
        };
    }

    public static List<String> fetchStatisticsTypeIndicators(StatisticsType statisticsType) {
        return switch (statisticsType) {
            case VIEW -> indicatorMappingConfiguration.offsets.views.keySet().stream().toList();
            case DOWNLOAD ->
                indicatorMappingConfiguration.offsets.downloads.keySet().stream().toList();
        };
    }

    public static List<String> fetchAllStatisticsIndicatorCodes() {
        var indicatorCodes = new HashSet<String>();
        indicatorMappingConfiguration.mappings.values().forEach(indicatorCodes::addAll);
        return indicatorCodes.stream().toList();
    }

    public static boolean isStatisticIndicatorCode(String code) {
        return fetchAllStatisticsIndicatorCodes().contains(code);
    }

    public static PublicationSeriesIndicatorMapping fetchPublicationSeriesCSVIndicatorMapping(
        String mappingName) {
        return indicatorMappingConfiguration.publicationSeriesCSVIndicatorMapping.getOrDefault(
            mappingName, null);
    }

    public static List<String> getIFTableContent() {
        return indicatorMappingConfiguration.ifTableContent;
    }

    public static List<String> getExclusionsForClass(String className) {
        var classNameParts = className.split("\\.");
        var name = classNameParts[classNameParts.length - 1];

        if (!indicatorMappingConfiguration.exclusions.containsKey(name)) {
            return List.of();
        }

        return indicatorMappingConfiguration.exclusions().get(name);
    }

    private record IndicatorMappingConfiguration(
        @JsonProperty(value = "mappings", required = true) Map<String, List<String>> mappings,
        @JsonProperty(value = "statisticOffsets", required = true) Offsets offsets,
        @JsonProperty(value = "statisticExclusions", required = true) Map<String, List<String>> exclusions,
        @JsonProperty(value = "publicationSeriesCSVIndicatorMapping", required = true) Map<String, PublicationSeriesIndicatorMapping> publicationSeriesCSVIndicatorMapping,
        @JsonProperty(value = "ifTableContent") List<String> ifTableContent
    ) {
    }

    public record PublicationSeriesIndicatorMapping(
        @JsonProperty(value = "source", required = true) String source,
        @JsonProperty(value = "yearParseRegex", required = true) String yearParseRegex,
        @JsonProperty(value = "nameColumn", required = true) Integer nameColumn,
        @JsonProperty(value = "eIssnColumn", required = true) Integer eIssnColumn,
        @JsonProperty(value = "editionColumn") Integer editionColumn,
        @JsonProperty(value = "printIssnColumn", required = true) Integer printIssnColumn,
        @JsonProperty(value = "identifierDelimiter") String identifierDelimiter,
        @JsonProperty(value = "categoryColumn", required = true) Integer categoryColumn,
        @JsonProperty(value = "categoryDelimiter") String categoryDelimiter,
        @JsonProperty(value = "categoryFromIndicatorDiffRegex") String categoryFromIndicatorDiffRegex,
        @JsonProperty(value = "defaultLanguage", required = true) String defaultLanguage,
        @JsonProperty(value = "columnMapping", required = true) Map<String, ColumnMapping> columnMapping,
        @JsonProperty(value = "parallelize", required = true) Boolean parallelize,
        @JsonProperty(value = "discriminator") String discriminator,
        @JsonProperty(value = "startDateColumn") Integer startDateColumn,
        @JsonProperty(value = "endDateColumn") Integer endDateColumn
    ) {
    }

    public record ColumnMapping(
        @JsonProperty(value = "mapsTo", required = true) String mapsTo,
        @JsonProperty(value = "type", required = true) EntityIndicatorType type,
        @JsonProperty(value = "parseRegex") String parseRegex
    ) {
    }

    private record Offsets(
        @JsonProperty(value = "views", required = true) Map<String, String> views,
        @JsonProperty(value = "downloads", required = true) Map<String, String> downloads
    ) {
    }
}

