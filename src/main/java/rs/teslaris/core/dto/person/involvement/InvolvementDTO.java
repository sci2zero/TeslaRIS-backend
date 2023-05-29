package rs.teslaris.core.dto.person.involvement;

import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.person.InvolvementType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvolvementDTO {

    private Integer id;

    @NotNull(message = "You must provide a valid starting date.")
    private LocalDate dateFrom;

    private LocalDate dateTo;

    private List<DocumentFileResponseDTO> proofs;

    @NotNull(message = "You must provide a valid involvement type.")
    private InvolvementType involvementType;

    @Valid
    private List<MultilingualContentDTO> affiliationStatement;

    @Positive(message = "Organisation unit ID must be a positive number.")
    private Integer organisationUnitId;
}
