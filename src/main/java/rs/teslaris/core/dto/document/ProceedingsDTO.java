package rs.teslaris.core.dto.document;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProceedingsDTO extends DocumentDTO {

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
