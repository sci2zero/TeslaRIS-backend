package rs.teslaris.core.assessment.ruleengine;

public class AssessmentPointsRuleEngine {

    public double pointsRulebook2025(String researchArea, String classificationCode) {
        switch (classificationCode) {
            case "docM21APlus":
                return 20;
            case "docM21A":
                return 12;
            case "docM21":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 8;
                } else {
                    return 10;
                }
        }
        return 0;
    }
}
