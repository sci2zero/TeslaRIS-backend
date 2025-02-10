package rs.teslaris.core.assessment.ruleengine;

import java.util.List;
import java.util.Objects;
import lombok.Setter;
import rs.teslaris.core.assessment.model.DocumentIndicator;
import rs.teslaris.core.assessment.model.EntityIndicator;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Setter
public class AssessmentPointsScalingRuleEngine {

    private List<DocumentIndicator> currentEntityIndicators;

    public double scalingRulebook2025(DocumentPublicationIndex personIndex,
                                      String classificationCode, double points) {
        var authorNumber = personIndex.getAuthorIds().size();
        var isExperimental = Objects.nonNull(findIndicatorByCode("isExperimental"));
        var isSimulation = Objects.nonNull(findIndicatorByCode("isSimulation"));
        var isM21aPlus = classificationCode.equals("docM21aPlus");
        var isTheoretical = Objects.nonNull(findIndicatorByCode("isTheoretical"));

        // Theoretical works (up to 3 authors, otherwise scale)
        if (isTheoretical) {
            if (authorNumber > 3) {
                return points / (1 + 0.2 * (authorNumber - 3));
            }
        }
        // Numerical simulations or primary data collection (up to 5 authors, otherwise scale)
        else if (isSimulation) {
            if (authorNumber > 5) {
                return points / (1 + 0.2 * (authorNumber - 5));
            }
        }
        // Experimental works (up to 7 authors, otherwise scale)
        else if (isExperimental) {
            if (authorNumber > 7) {
                return points / (1 + 0.2 * (authorNumber - 7));
            }
        }
        // M21a+ category (up to 10 authors, otherwise scale)
        else if (isM21aPlus) {
            if (authorNumber > 10) {
                return points / (1 + 0.2 * (authorNumber - 10));
            }
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
