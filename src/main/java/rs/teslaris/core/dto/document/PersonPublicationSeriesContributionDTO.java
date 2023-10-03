package rs.teslaris.core.dto.document;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonPublicationSeriesContributionDTO extends PersonContributionDTO {

    private PublicationSeriesContributionType contributionType;

    private LocalDate dateFrom;

    private LocalDate dateTo;
}
