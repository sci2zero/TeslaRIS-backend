package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.ThesisType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisDTO extends DocumentDTO {

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer organisationUnitId;

    private List<MultilingualContentDTO> externalOrganisationUnitName;

    @NotNull(message = "You have to provide thesis type.")
    private ThesisType thesisType;

    @Positive(message = "Number of pages cannot be a negative number.")
    private Integer numberOfPages;

    @Positive
    private Integer languageId;

    @Positive
    private Integer writingLanguageTagId;

    @Positive(message = "Research area id cannot be a negative number.")
    private Integer researchAreaId;

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;

    private LocalDate topicAcceptanceDate;
}
