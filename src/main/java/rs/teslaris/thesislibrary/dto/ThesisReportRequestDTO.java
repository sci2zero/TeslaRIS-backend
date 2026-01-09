package rs.teslaris.thesislibrary.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.model.document.ThesisType;

public record ThesisReportRequestDTO(

    @NotNull(message = "You have to provide start date.")
    LocalDate fromDate,

    @NotNull(message = "You have to provide end date.")
    LocalDate toDate,

    @NotNull(message = "You have to provide top level institution IDs tokens.")
    List<Integer> topLevelInstitutionIds,

    @NotNull(message = "You have to provide thesis type.")
    ThesisType thesisType
) {
}
