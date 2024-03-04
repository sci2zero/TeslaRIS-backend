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
public class PrizeResponseDTO {

    private List<MultilingualContentDTO> title;

    private List<MultilingualContentDTO> description;

    private List<DocumentFileResponseDTO> proofs;

    private LocalDate date;
}
