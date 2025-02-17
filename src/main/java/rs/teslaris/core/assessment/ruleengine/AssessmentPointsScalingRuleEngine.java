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

    public double serbianScalingRulebook2025(DocumentPublicationIndex publicationIndex,
                                             String classificationCode, Double points) {
        var authorNumber = publicationIndex.getAuthorIds().size();
        var revisedAuthorNumber = findIndicatorByCode("revisedAuthorCount");
        if (Objects.nonNull(revisedAuthorNumber)) {
            authorNumber = revisedAuthorNumber.getNumericValue().intValue();
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
            return points;
        }

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
        // Experimental works or M80 category (up to 7 authors, otherwise scale)
        else if (isExperimental || isM80) {
            if (authorNumber > 7) {
                return points / (1 + 0.2 * (authorNumber - 7));
            }
        }
        // M21a+ or M90 category (up to 10 authors, otherwise scale)
        else if (isM21aPlus || isM90) {
            if (authorNumber > 10) {
                return points / (1 + 0.2 * (authorNumber - 10));
            }
        }

        // Treat it as experimental by default
        if (authorNumber > 7) {
            return points / (1 + 0.2 * (authorNumber - 7));
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
