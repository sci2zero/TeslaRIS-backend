package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Getter
@Setter
@Entity
@Table(name = "monographs")
@Where(clause = "deleted=false")
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

    @Column(name = "edition_title")
    private String editionTitle;

    @Column(name = "edition_number")
    private Integer editionNumber;

    @Column(name = "edition_issn", unique = false)
    private String editionISSN;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_id")
    private ResearchArea researchArea;
}
