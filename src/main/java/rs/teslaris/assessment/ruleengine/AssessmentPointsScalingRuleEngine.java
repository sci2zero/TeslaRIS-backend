package rs.teslaris.assessment.ruleengine;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EntityIndicator;
import rs.teslaris.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Setter
public class AssessmentPointsScalingRuleEngine {

    private List<DocumentIndicator> currentEntityIndicators;

    private String publicationType;

    @Getter
    private Set<MultiLingualContent> reasoningProcess = new HashSet<>();


    public double serbianScalingRulebook2025(Integer authorCount,
                                             String classificationCode,
                                             Double points) {
        reasoningProcess.clear();

        var revisedAuthorNumber = findIndicatorByCode("revisedAuthorCount");
        if (Objects.nonNull(revisedAuthorNumber)) {
            authorCount = revisedAuthorNumber.getNumericValue().intValue();
        }

        var isExperimental = Objects.nonNull(findIndicatorByCode("isExperimental"));
        var isSimulation = Objects.nonNull(findIndicatorByCode("isSimulation"));
        var isM21aPlus = classificationCode.equals("M21APlus");
        var isM80 = classificationCode.startsWith("M8");
        var isM90 = classificationCode.startsWith("M9");
        var isM10OrM40 = classificationCode.startsWith("M1") || classificationCode.startsWith("M4");
        var isTheoretical = Objects.nonNull(findIndicatorByCode("isTheoretical"));

        // No scaling for M10 and M40
        if (isM10OrM40) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                    "m10OrM40Results");
            return points;
        }

        if (Objects.nonNull(publicationType) &&
            List.of("SCIENTIFIC_CRITIC", "POLEMICS", "COMMENT").contains(publicationType)) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                    "notFullResults");
            points = points * 0.25;
        }

        // Theoretical works (up to 3 authors, otherwise scale)
        if (isTheoretical) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                    "theoreticalResults");
            if (authorCount > 3) {
                return points / (1 + 0.2 * (authorCount - 3));
            }
            return points;
        }
        // Numerical simulations or primary data collection (up to 5 authors, otherwise scale)
        else if (isSimulation) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                    "simulationResults");
            if (authorCount > 5) {
                return points / (1 + 0.2 * (authorCount - 5));
            }
            return points;
        }
        // Experimental works or M80 category (up to 7 authors, otherwise scale)
        else if (isExperimental || isM80) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                    "experimentalOrM80Results");
            if (authorCount > 7) {
                return points / (1 + 0.2 * (authorCount - 7));
            }
            return points;
        }
        // M21a+ or M90 category (up to 10 authors, otherwise scale)
        else if (isM21aPlus || isM90) {
            reasoningProcess =
                AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                    "m21APlusOrM90Results");
            if (authorCount > 10) {
                return points / (1 + 0.2 * (authorCount - 10));
            }
            return points;
        }

        // Treat it as experimental by default
        reasoningProcess =
            AssessmentRulesConfigurationLoader.getRuleDescription("scalingRules",
                "experimentalOrM80Results");
        if (authorCount > 7) {
            return points / (1 + 0.2 * (authorCount - 7));
        }

        return points;
    }

    private EntityIndicator findIndicatorByCode(String code) {
        return currentEntityIndicators.stream()
            .filter(journalIndicator ->
                journalIndicator.getIndicator().getCode().equals(code))
            .findFirst()
            .orElse(null);
    }
}
