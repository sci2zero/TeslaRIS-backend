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
public class EventDTO {

    private Integer id;

    @NotNull(message = "You have to provide name.")
    private List<MultilingualContentDTO> name;

    @NotNull(message = "You have to provide nameAbbreviation.")
    private List<MultilingualContentDTO> nameAbbreviation;

    @NotNull(message = "You have to provide description.")
    private List<MultilingualContentDTO> description;

    @NotNull(message = "You have to provide keywords.")
    private List<MultilingualContentDTO> keywords;

    @NotNull(message = "You have to provide serial event information.")
    private Boolean serialEvent;

    @NotNull(message = "You have to provide start date.")
    private LocalDate dateFrom;

    @NotNull(message = "You have to provide end date.")
    private LocalDate dateTo;

    @NotNull(message = "You have to provide state.")
    private List<MultilingualContentDTO> state;

    @NotNull(message = "You have to provide place.")
    private List<MultilingualContentDTO> place;

    @Valid
    private List<PersonEventContributionDTO> contributions;
}
