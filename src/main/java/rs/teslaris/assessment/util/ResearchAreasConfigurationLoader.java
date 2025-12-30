package rs.teslaris.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;

@Component
public class ResearchAreasConfigurationLoader {

    private static ResearchAreaConfiguration researchAreaConfiguration = null;

    private static LanguageTagService languageTagService;

    private static String externalOverrideConfiguration;


    @Autowired
    public ResearchAreasConfigurationLoader(LanguageTagService languageTagService,
                                            @Value("${assessment.research-areas.mapping}")
                                            String externalOverrideConfiguration) {
        ResearchAreasConfigurationLoader.languageTagService = languageTagService;
        ResearchAreasConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            researchAreaConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                ResearchAreaConfiguration.class,
                "src/main/resources/assessment/assessmentResearchAreasConfiguration.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload classification mapping configuration: " + e.getMessage());
        }
    }

    public static boolean codeExists(String code) {
        return researchAreaConfiguration.researchAreas.stream()
            .anyMatch(researchArea -> researchArea.code().equals(code));
    }

    public static List<AssessmentResearchAreaDTO> fetchAllAssessmentResearchAreas() {
        var researchAreas = new ArrayList<AssessmentResearchAreaDTO>();
        researchAreaConfiguration.researchAreas.forEach((researchArea) -> {
            var researchAreaResponse = new AssessmentResearchAreaDTO();
            researchAreaResponse.setCode(researchArea.code());
            researchAreaResponse.setName(new ArrayList<>());

            var priority = new AtomicInteger(1);
            researchArea.name().forEach((languageCode, content) -> {
                var languageTag = languageTagService.findLanguageTagByValue(languageCode);
                researchAreaResponse.getName()
                    .add(new MultilingualContentDTO(languageTag.getId(), languageCode, content,
                        priority.getAndIncrement()));
            });

            researchAreas.add(researchAreaResponse);
        });

        return researchAreas;
    }

    public static List<MultilingualContentDTO> fetchAssessmentResearchAreaNameByCode(
        String researchAreaCode) {
        var researchAreaName = new ArrayList<MultilingualContentDTO>();

        researchAreaConfiguration.researchAreas.stream()
            .filter(area -> area.code().equals(researchAreaCode)).findFirst()
            .ifPresent(researchArea -> {
                var priority = new AtomicInteger(1);
                researchArea.name().forEach((languageCode, content) -> {
                    var languageTag = languageTagService.findLanguageTagByValue(languageCode);
                    researchAreaName.add(
                        new MultilingualContentDTO(languageTag.getId(), languageCode, content,
                            priority.getAndIncrement()));
                });
            });

        return researchAreaName;
    }

    public static Optional<AssessmentResearchAreaDTO> fetchAssessmentResearchAreaByCode(
        String code) {
        var researchArea = researchAreaConfiguration.researchAreas.stream()
            .filter(area -> area.code().equals(code)).findFirst();

        if (researchArea.isEmpty()) {
            return Optional.empty();
        }

        var researchAreaResponse = new AssessmentResearchAreaDTO();
        researchAreaResponse.setCode(researchArea.get().code());
        researchAreaResponse.setName(new ArrayList<>());

        var priority = new AtomicInteger(1);
        researchArea.get().name().forEach((languageCode, content) -> {
            var languageTag = languageTagService.findLanguageTagByValue(languageCode);
            researchAreaResponse.getName()
                .add(new MultilingualContentDTO(languageTag.getId(), languageCode, content,
                    priority.getAndIncrement()));
        });

        return Optional.of(researchAreaResponse);
    }

    public static List<Pair<String, Triple<Integer, List<String>, String>>> getResearchAreaClassificationsMappings() {
        return researchAreaConfiguration.researchAreaClassificationsMappings.stream().map(
            mapping ->
                new Pair<>(mapping.code, new Triple<>(mapping.topLevelSubAreaId,
                    mapping.allowedClassifications, mapping.fallback))).toList();
    }

    private record ResearchAreaConfiguration(
        @JsonProperty(value = "researchAreas", required = true) List<ResearchArea> researchAreas,

        @JsonProperty(value = "researchAreaClassificationsMappings", required = true)
        List<ResearchAreaClassificationsMapping> researchAreaClassificationsMappings
    ) {
    }

    private record ResearchArea(
        @JsonProperty(value = "name", required = true) Map<String, String> name,
        @JsonProperty(value = "code", required = true) String code
    ) {
    }

    private record ResearchAreaClassificationsMapping(
        @JsonProperty(value = "code", required = true) String code,
        @JsonProperty(value = "topLevelSubAreaId", required = true) Integer topLevelSubAreaId,
        @JsonProperty(value = "allowedClassifications", required = true) List<String> allowedClassifications,
        @JsonProperty(value = "fallback", required = true) String fallback
    ) {
    }
}
