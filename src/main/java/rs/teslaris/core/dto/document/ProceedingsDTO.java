package rs.teslaris.core.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsDTO extends DocumentDTO {

    @JsonProperty("eISBN")
    private String eISBN;

    private String printISBN;

    private Integer numberOfPages;

    @NotNull(message = "You have to provide languages.")
    private List<Integer> languageTagIds;

    private Integer publisherId;

    private Integer publicationSeriesId;

    private String publicationSeriesVolume;

    private String publicationSeriesIssue;
}
