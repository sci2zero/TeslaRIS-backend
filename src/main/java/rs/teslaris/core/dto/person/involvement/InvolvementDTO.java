package rs.teslaris.core.dto.person.involvement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
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

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private List<DocumentFileResponseDTO> proofs;

    @NotNull(message = "You must provide a valid involvement type.")
    private InvolvementType involvementType;

    @Valid
    private List<MultilingualContentDTO> affiliationStatement;

    private Integer organisationUnitId;

    // Only for responses
    private List<MultilingualContentDTO> organisationUnitName;
}
