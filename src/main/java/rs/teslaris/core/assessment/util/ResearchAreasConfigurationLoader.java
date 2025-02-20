package rs.teslaris.core.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

@Component
public class ResearchAreasConfigurationLoader {

    private static ResearchAreaConfiguration researchAreaConfiguration = null;

    private static LanguageTagService languageTagService;

    @Autowired
    public ResearchAreasConfigurationLoader(LanguageTagService languageTagService) {
        ResearchAreasConfigurationLoader.languageTagService = languageTagService;
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    private static void reloadConfiguration() {
        try {
            loadConfiguration();
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload classification mapping configuration: " + e.getMessage());
        }
    }

    private static synchronized void loadConfiguration() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        researchAreaConfiguration = objectMapper.readValue(
            new FileInputStream(
                "src/main/resources/assessment/assessmentResearchAreasConfiguration.json"),
            ResearchAreaConfiguration.class
        );
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

    private record ResearchAreaConfiguration(
        @JsonProperty(value = "researchAreas", required = true) List<ResearchArea> researchAreas
    ) {
    }

    private record ResearchArea(
        @JsonProperty(value = "name", required = true) Map<String, String> name,
        @JsonProperty(value = "code", required = true) String code
    ) {
    }
}
