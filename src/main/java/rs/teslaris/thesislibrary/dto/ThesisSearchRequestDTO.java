package rs.teslaris.thesislibrary.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.model.document.ThesisType;

public record ThesisSearchRequestDTO(
    @NotNull(message = "You have to provide search tokens.")
    List<String> tokens,
    List<Integer> facultyIds,
    List<Integer> authorIds,
    List<Integer> advisorIds,
    List<Integer> boardMemberIds,
    List<Integer> boardPresidentIds,
    List<ThesisType> thesisTypes,
    Boolean showOnlyOpenAccess,
    LocalDate dateFrom,
    LocalDate dateTo
) {
}
