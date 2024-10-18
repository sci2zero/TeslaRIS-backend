package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

    private Integer oldId;

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

    private LocalDate dateFrom;

    private LocalDate dateTo;

    @NotNull(message = "You have to provide state.")
    private List<MultilingualContentDTO> state;

    @NotNull(message = "You have to provide place.")
    private List<MultilingualContentDTO> place;

    @NotNull(message = "You have to provide contribution list.")
    private List<PersonEventContributionDTO> contributions;

    private Set<String> uris;
}
