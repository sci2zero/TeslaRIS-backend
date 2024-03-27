package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrizeResponseDTO extends PrizeDTO {

    private Integer id;

    private List<DocumentFileResponseDTO> proofs;

    public PrizeResponseDTO(List<MultilingualContentDTO> title,
                            List<MultilingualContentDTO> description,
                            LocalDate date, Integer id,
                            List<DocumentFileResponseDTO> proofs) {
        super(title, description, date);
        this.id = id;
        this.proofs = proofs;
    }
}
