package rs.teslaris.assessment.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ConfigurationLoaderUtil;
import rs.teslaris.core.util.search.StringUtil;

@Component
public class AssessmentRulesConfigurationLoader {

    private static AssessmentRulesConfigurationLoader.AssessmentRulesConfiguration
        assessmentRulesConfiguration = null;

    private static String externalOverrideConfiguration;

    private static LanguageTagService languageTagService;

    @Autowired
    public AssessmentRulesConfigurationLoader(LanguageTagService languageTagService,
                                              @Value("${assessment.rules.configuration}")
                                              String externalOverrideConfiguration) {
        AssessmentRulesConfigurationLoader.languageTagService = languageTagService;
        AssessmentRulesConfigurationLoader.externalOverrideConfiguration =
            externalOverrideConfiguration;
        reloadConfiguration();
    }

    @Scheduled(fixedRate = (1000 * 60 * 10)) // 10 minutes
    protected static void reloadConfiguration() {
        try {
            assessmentRulesConfiguration = ConfigurationLoaderUtil.loadConfiguration(
                AssessmentRulesConfigurationLoader.AssessmentRulesConfiguration.class,
                "src/main/resources/assessment/assessmentRules.json",
                externalOverrideConfiguration);
        } catch (IOException e) {
            throw new StorageException(
                "Failed to reload assessment rules configuration: " + e.getMessage());
        }
    }

    public static Set<MultiLingualContent> getRuleDescription(String ruleGroupCode, String ruleCode,
                                                              Object... params) {
        Map<String, String> localizedContent =
            assessmentRulesConfiguration
                .ruleDescriptions
                .get(ruleGroupCode)
                .get(ruleCode);

        if (ruleGroupCode.equals("journalClassificationRules")) {
            localizedContent = localizedContent.entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        String prefix =
                            assessmentRulesConfiguration.ruleDescriptions
                                .get(ruleGroupCode)
                                .get(ruleCode.endsWith("MNO")
                                    ? "mnoAssessmentRulePrefix"
                                    : "generalAssessmentRulePrefix")
                                .get(entry.getKey());

                        return prefix + "§" + entry.getValue();
                    }));
        }

        return StringUtil.buildMultilingualContent(languageTagService, localizedContent, params);
    }

    private record AssessmentRulesConfiguration(
        @JsonProperty(value = "ruleDescriptions", required = true) Map<String, Map<String, Map<String, String>>> ruleDescriptions
    ) {
    }
}
