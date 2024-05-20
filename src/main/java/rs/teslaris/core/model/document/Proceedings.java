package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
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

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages = new HashSet<>();

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
