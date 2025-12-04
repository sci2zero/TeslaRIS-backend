package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "proceedings", indexes = {
    @Index(name = "idx_proceedings_e_isbn", columnList = "e_isbn"),
    @Index(name = "idx_proceedings_print_isbn", columnList = "print_isbn")
})
@SQLRestriction("deleted=false")
public non-sealed class Proceedings extends Document
    implements BookSeriesPublishable, PublisherPublishable {

    @Column(name = "e_isbn")
    private String eISBN;

    @Column(name = "print_isbn")
    private String printISBN;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Language> languages = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Column(name = "author_reprint")
    private Boolean authorReprint = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_series_id")
    private PublicationSeries publicationSeries;

    @Column(name = "publication_series_volume")
    private String publicationSeriesVolume;

    @Column(name = "publication_series_issue")
    private String publicationSeriesIssue;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> nameAbbreviation = new HashSet<>();
}
