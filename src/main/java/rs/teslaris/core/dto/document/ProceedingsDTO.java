package rs.teslaris.core.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
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
public class ProceedingsDTO extends DocumentDTO implements PublishableDTO, InSeriesDTO {

    @JsonProperty("eISBN")
    private String eISBN;

    private String printISBN;

    private Integer numberOfPages;

    @NotNull(message = "You have to provide languages.")
    private List<Integer> languageTagIds;

    @Positive(message = "Publisher ID cannot be negative or zero.")
    private Integer publisherId;

    private Boolean authorReprint;

    private Integer publicationSeriesId;

    private String publicationSeriesVolume;

    private String publicationSeriesIssue;

    private List<MultilingualContentDTO> acronym = new ArrayList<>();
}
