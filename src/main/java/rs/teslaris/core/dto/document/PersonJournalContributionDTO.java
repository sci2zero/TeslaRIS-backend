package rs.teslaris.core.dto.document;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.JournalContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonJournalContributionDTO extends PersonContributionDTO {

    private JournalContributionType contributionType;

    private LocalDate dateFrom;

    private LocalDate dateTo;
}
