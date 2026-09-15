package rs.teslaris.revisioner.dto;

import java.util.List;

public record IssueStatisticsDTO(

    long openIssues,

    long errorIssues,

    long warningIssues,

    long infoIssues,

    List<SeverityBreakdownDTO> issuesBySeverityAndEntityType,

    List<PrevalentIssueDTO> topRecurringConstraints
) {
}
