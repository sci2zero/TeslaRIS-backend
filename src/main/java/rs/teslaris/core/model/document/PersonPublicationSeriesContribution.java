package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;


@Getter
@Setter
@Entity
@Table(name = "person_journal_contributions")
@Where(clause = "deleted=false")
public class PersonPublicationSeriesContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    private PublicationSeriesContributionType contributionType;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_series_id", nullable = false)
    private PublicationSeries publicationSeries;
}
