package rs.teslaris.revisioner.model.qualityassessment;

public record DimensionScore(
    double achievedPoints,
    double totalPoints,
    double percentage
) {
}
