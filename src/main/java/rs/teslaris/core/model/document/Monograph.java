package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Getter
@Setter
@Entity
@Table(name = "monographs", indexes = {
    @Index(name = "idx_monograph_e_isbn", columnList = "e_isbn"),
    @Index(name = "idx_monograph_print_isbn", columnList = "print_isbn")
})
@SQLRestriction("deleted=false")
public non-sealed class Monograph extends Document
    implements BookSeriesPublishable, PublisherPublishable {

    @Column(name = "monograph_type", nullable = false)
    @ColumnDefault("0")
    @Enumerated(value = EnumType.ORDINAL)
    private MonographType monographType;

    @Column(name = "print_isbn")
    private String printISBN;

    @Column(name = "e_isbn")
    private String eISBN;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "volume")
    private String volume;

    @Column(name = "monograph_number")
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_series_id")
    private PublicationSeries publicationSeries;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Language> languages = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_id")
    private ResearchArea researchArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Column(name = "author_reprint")
    private Boolean authorReprint = false;
}
