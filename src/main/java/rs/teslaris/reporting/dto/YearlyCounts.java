package rs.teslaris.reporting.dto;

import java.util.Map;

public record YearlyCounts(
    Integer year,
    Map<String, Long> countsByCategory
) {
}
