package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Getter
@Setter
@Entity
@Table(name = "monographs")
@SQLRestriction("deleted=false")
public class Monograph extends Document {

    @Column(name = "monograph_type", nullable = false)
    @ColumnDefault("0")
    @Enumerated(value = EnumType.ORDINAL)
    private MonographType monographType;

    @Column(name = "print_isbn", unique = true)
    private String printISBN;

    @Column(name = "e_isbn", unique = true)
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

    @OneToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_id")
    private ResearchArea researchArea;
}
