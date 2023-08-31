package rs.teslaris.core.dto.document;

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
public class EventDTO {

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> nameAbbreviation;

    private List<MultilingualContentDTO> description;

    private List<MultilingualContentDTO> keywords;

    private Boolean serialEvent;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private List<MultilingualContentDTO> state;

    private List<MultilingualContentDTO> place;

    private List<PersonEventContributionDTO> contributions;
}
