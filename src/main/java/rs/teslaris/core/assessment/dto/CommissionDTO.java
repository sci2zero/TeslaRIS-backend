package rs.teslaris.core.assessment.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

public record CommissionDTO(

    Integer id,

    @Valid
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    List<MultilingualContentDTO> description,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    List<String> sources,

    @NotNull(message = "You have to provide the start of assessment period.")
    LocalDate assessmentDateFrom,

    @NotNull(message = "You have to provide the end of assessment period.")
    LocalDate assessmentDateTo,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    List<Integer> documentIdsForAssessment,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    List<Integer> personIdsForAssessment,

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    List<Integer> organisationUnitIdsForAssessment,

    @NotBlank(message = "You have to provide formal rule description.")
    String formalDescriptionOfRule,

    @NotBlank(message = "You have to provide recognised research areas.")
    List<String> recognisedResearchAreas
) {
}
