package rs.teslaris.core.dto.person.involvement;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.person.InvolvementType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvolvementDTO {

    private LocalDate dateFrom;

    private LocalDate dateTo;

    // TODO: ADD DOCUMENT PROOFS

    private InvolvementType involvementType;

    private List<MultilingualContentDTO> affiliationStatement;

    private Integer organisationUnitId;
}
