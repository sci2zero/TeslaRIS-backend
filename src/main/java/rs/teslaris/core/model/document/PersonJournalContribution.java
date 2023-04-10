package rs.teslaris.core.model.document;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "person_journal_contributions")
public class PersonJournalContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    JournalContributionType contributionType;

    @Column(name = "from", nullable = false)
    LocalDate from;

    @Column(name = "to", nullable = false)
    LocalDate to;
}
