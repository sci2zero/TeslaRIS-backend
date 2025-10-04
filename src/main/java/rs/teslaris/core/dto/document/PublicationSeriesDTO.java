package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
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
public class PublicationSeriesDTO {

    private Integer id;

    private Integer oldId;

    @NotNull(message = "You have to provide a title.")
    private List<MultilingualContentDTO> title;

    private String eissn;

    private String printISSN;

    private String openAlexId;

    @NotNull(message = "You have to provide contributions.")
    private List<PersonPublicationSeriesContributionDTO> contributions;

    @NotNull(message = "You have to provide language tags.")
    private List<Integer> languageTagIds;

    @NotNull(message = "You have to provide name abbreviations.")
    private List<MultilingualContentDTO> nameAbbreviation;

    private Set<String> uris;

    private List<MultilingualContentDTO> subtitle = new ArrayList<>();
}
