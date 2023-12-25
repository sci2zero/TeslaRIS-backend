package rs.teslaris.core.dto.document;

import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceBasicAdditionDTO {

    private Integer id;

    @Valid
    @NotNull(message = "You have to provide conference name.")
    private List<MultilingualContentDTO> name;

    @NotNull(message = "You have to provide at least a year when this conference took place.")
    private LocalDate dateFrom;

    private LocalDate dateTo;
}
