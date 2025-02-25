package rs.teslaris.core.assessment.ruleengine;

import java.util.List;
import java.util.Objects;
import lombok.Setter;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.assessment.model.EntityIndicator;

@Setter
public class AssessmentPointsScalingRuleEngine {

    private List<DocumentIndicator> currentEntityIndicators;

    public double serbianScalingRulebook2025(Integer authorCount,
                                             String classificationCode, Double points) {
        var revisedAuthorNumber = findIndicatorByCode("revisedAuthorCount");
        if (Objects.nonNull(revisedAuthorNumber)) {
            authorCount = revisedAuthorNumber.getNumericValue().intValue();
        }

        var isExperimental = Objects.nonNull(findIndicatorByCode("isExperimental"));
        var isSimulation = Objects.nonNull(findIndicatorByCode("isSimulation"));
        var isM21aPlus = classificationCode.equals("M21aPlus");
        var isTheoretical = Objects.nonNull(findIndicatorByCode("isTheoretical"));

        // Theoretical works (up to 3 authors, otherwise scale)
        if (isTheoretical) {
            if (authorCount > 3) {
                return points / (1 + 0.2 * (authorCount - 3));
            }
        }
        // Numerical simulations or primary data collection (up to 5 authors, otherwise scale)
        else if (isSimulation) {
            if (authorCount > 5) {
                return points / (1 + 0.2 * (authorCount - 5));
            }
        }
        // Experimental works (up to 7 authors, otherwise scale)
        else if (isExperimental) {
            if (authorCount > 7) {
                return points / (1 + 0.2 * (authorCount - 7));
            }
        }
        // M21a+ category (up to 10 authors, otherwise scale)
        else if (isM21aPlus) {
            if (authorCount > 10) {
                return points / (1 + 0.2 * (authorCount - 10));
            }
        }

        // Treat it as experimental by default
        if (authorCount > 7) {
            return points / (1 + 0.2 * (authorCount - 7));
        } else {
            return points;
        }
    }

    private EntityIndicator findIndicatorByCode(String code) {
        return currentEntityIndicators.stream()
            .filter(journalIndicator ->
                journalIndicator.getIndicator().getCode().equals(code))
            .findFirst()
            .orElse(null);
    }
}
