package rs.teslaris.core.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;

@Component
public class AssessmentRulesConfigurationLoader {

    private static AssessmentRulesConfigurationLoader.AssessmentRulesConfiguration
        assessmentRulesConfiguration = null;

    private static LanguageTagService languageTagService;

    @Autowired
    public AssessmentRulesConfigurationLoader(LanguageTagService languageTagService) {
        AssessmentRulesConfigurationLoader.languageTagService = languageTagService;
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
        assessmentRulesConfiguration = objectMapper.readValue(
            new FileInputStream(
                "src/main/resources/assessment/assessmentRules.json"),
            AssessmentRulesConfigurationLoader.AssessmentRulesConfiguration.class
        );
    }

    public static Set<MultiLingualContent> getRuleDescription(String ruleGroupCode,
                                                              String ruleCode,
                                                              Object... params) {
        var ruleDescription = new HashSet<MultiLingualContent>();
        AtomicInteger priority = new AtomicInteger(1);

        assessmentRulesConfiguration.ruleDescriptions.get(ruleGroupCode).get(ruleCode)
            .forEach((languageCode, content) -> {
                var mc = new MultiLingualContent();
                mc.setLanguage(
                    languageTagService.findLanguageTagByValue(languageCode.toUpperCase()));

                if (Objects.isNull(mc.getLanguage().getLanguageTag())) {
                    return;
                }

                Object[] processedParams = Arrays.stream(params)
                    .map(param -> {
                        if (param instanceof List<?> list) {
                            return list.stream()
                                .filter(item -> item instanceof MultilingualContentDTO)
                                .map(item -> (MultilingualContentDTO) item)
                                .filter(dto -> dto.getLanguageTag()
                                    .equals(languageCode)) // Match language
                                .map(MultilingualContentDTO::getContent)
                                .findFirst() // Take the first match
                                .orElse(""); // Default if not found
                        }
                        return param;
                    })
                    .toArray();

                mc.setContent(MessageFormat.format(content, processedParams));
                mc.setPriority(priority.getAndAdd(1));
                ruleDescription.add(mc);
            });

        return ruleDescription;
    }

    private record AssessmentRulesConfiguration(
        @JsonProperty(value = "ruleDescriptions", required = true) Map<String, Map<String, Map<String, String>>> ruleDescriptions
    ) {
    }
}
