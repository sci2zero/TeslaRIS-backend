package rs.teslaris.core.assessment.ruleengine;

public class AssessmentPointsRuleEngine {

    public double serbianPointsRulebook2025(String researchArea, String classificationCode) {
        switch (classificationCode) {
            case "docM21APlus":
                return 20;
            case "docM21A":
                return 12;
            case "docM21":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 8;
                }
                return 10;
            case "docM22":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 5;
                }
                return 6;
            case "docM23":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 3;
                } else if (researchArea.equals("SOCIAL")) {
                    return 5;
                }
                return 4;
            case "docM23e":
                if (researchArea.equals("HUMANITIES")) {
                    return 4;
                } else if (researchArea.equals("SOCIAL")) {
                    return 2;
                }
                break;
            case "docM24Plus":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 2;
                }
                return 4;
            case "docM24":
                if (researchArea.equals("NATURAL")) {
                    return 2;
                } else if (researchArea.equals("TECHNICAL") || researchArea.equals("SOCIAL")) {
                    return 3;
                }
                return 4;
            case "M26":
                return 1;
            case "M27":
                if (researchArea.equals("HUMANITIES")) {
                    return 1;
                }
                return 0.5;
            case "docM51":
                if (researchArea.equals("SOCIAL")) {
                    return 2.5;
                }
                if (researchArea.equals("HUMANITIES")) {
                    return 3;
                }
                return 2;
            case "docM52":
                if (researchArea.equals("HUMANITIES")) {
                    return 2;
                }
                return 1.5;
            case "docM53":
                return 1;
            case "docM54":
                return 0.5;
            case "M56":
                if (researchArea.equals("HUMANITIES")) {
                    return 0.5;
                }
                return 0.3;
            case "M57":
                if (researchArea.equals("HUMANITIES")) {
                    return 0.3;
                }
                return 0.2;

            // PROCEEDINGS PUBLICATIONS
            case "M31":
                return 3.5;
            case "M34", "M64":
                return 0.5;
            case "M32", "M61":
                return 1.5;
            case "M33", "M62", "M63":
                return 1;
            case "M67":
                if (researchArea.equals("HUMANITIES")) {
                    return 5;
                }
                break;
            case "M68":
                if (researchArea.equals("HUMANITIES")) {
                    return 2;
                }
                break;

            // MONOGRAPHS
            case "M11":
                return 15;
            case "M12":
                if (researchArea.equals("HUMANITIES")) {
                    return 12;
                }
                return 10;
            case "M13":
                if (researchArea.equals("HUMANITIES")) {
                    return 7;
                }
                if (researchArea.equals("SOCIAL")) {
                    return 6;
                }
                return 5;
            case "M14":
                if (researchArea.equals("HUMANITIES")) {
                    return 5;
                }
                if (researchArea.equals("SOCIAL")) {
                    return 4;
                }
                return 3;
            case "M15":
                if (researchArea.equals("HUMANITIES")) {
                    return 3;
                }
                return 2;
            case "M16":
                if (researchArea.equals("HUMANITIES")) {
                    return 2;
                }
                return 1;
            case "M41":
                if (researchArea.equals("HUMANITIES")) {
                    return 10;
                }
                if (researchArea.equals("SOCIAL")) {
                    return 9;
                }
                return 7;
            case "M42":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 5;
                }
                return 7;
            case "M43":
                if (researchArea.equals("HUMANITIES")) {
                    return 5;
                }
                return 3;
            case "M44":
                if (researchArea.equals("HUMANITIES")) {
                    return 3;
                }
                if (researchArea.equals("SOCIAL")) {
                    return 2.5;
                }
                return 2;
            case "M45":
                if (researchArea.equals("HUMANITIES")) {
                    return 2;
                }
                return 1.5;
            case "M46":
                if (researchArea.equals("HUMANITIES")) {
                    return 1.5;
                }
                return 1;
            case "M47":
                if (researchArea.equals("HUMANITIES")) {
                    return 1;
                }
                return 0.5;

            // THESIS
            case "M70":
                return 6;

            // SOFTWARE, DATASET
            case "M81":
                return 12;
            case "M82":
                return 8;
            case "M83":
                return 6;
            case "M84":
                return 3;

            // PATENT
            case "M91a":
                return 30;
            case "M91":
                return 20;
            case "M92":
                return 14;
            case "M93":
                return 8;
            case "M94":
                return 4;
            case "M95":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 16;
                }
                break;
            case "M96":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 12;
                }
                break;
            case "M97":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 10;
                }
                break;
            case "M98":
                if (researchArea.equals("NATURAL") || researchArea.equals("TECHNICAL")) {
                    return 8;
                }
        }

        // Sometimes, your work is worthless :( but it does not mean it's meaningless :)
        return 0;
    }
}
