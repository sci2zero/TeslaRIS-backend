package rs.teslaris.core.dto.document;

import java.util.List;
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
public class PublicationSeriesDTO {

    private Integer id;

    private Integer oldId;

    @NotNull(message = "You have to provide title.")
    private List<MultilingualContentDTO> title;

    private String eissn;

    private String printISSN;

    @NotNull(message = "You have to provide contributions.")
    private List<PersonPublicationSeriesContributionDTO> contributions;

    @NotNull(message = "You have to provide language tags.")
    private List<Integer> languageTagIds;

    @NotNull(message = "You have to provide name abbreviations.")
    private List<MultilingualContentDTO> nameAbbreviation;
}
