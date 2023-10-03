package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.LanguageTag;

@Getter
@Setter
@Entity
@Table(name = "proceedings")
@Where(clause = "deleted=false")
public class Proceedings extends Document {

    @Column(name = "e_isbn")
    private String eISBN;

    @Column(name = "print_isbn")
    private String printISBN;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_series_id")
    private PublicationSeries publicationSeries;

    @Column(name = "publication_series_volume")
    private String publicationSeriesVolume;

    @Column(name = "publication_series_issue")
    private String publicationSeriesIssue;
}
