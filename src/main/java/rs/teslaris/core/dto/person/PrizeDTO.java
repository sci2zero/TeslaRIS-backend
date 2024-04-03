package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrizeDTO {

    private List<MultilingualContentDTO> title;

    private List<MultilingualContentDTO> description;

    private LocalDate date;
}
