package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.model.document.ThesisType;

public record ThesisReportRequestDTO(
    LocalDate fromDate,
    LocalDate toDate,
    List<Integer> topLevelInstitutionIds,
    ThesisType thesisType
) {
}
