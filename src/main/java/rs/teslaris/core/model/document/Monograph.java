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
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Getter
@Setter
@Entity
@Table(name = "monographs")
public class Monograph extends Document {

    @Column(name = "monograph_type")
    MonographType monographType;

    @Column(name = "print_isbn", unique = true)
    String printISBN;

    @Column(name = "e_isbn", unique = true)
    String eISBN;

    @Column(name = "number_of_pages")
    Integer numberOfPages;

    @Column(name = "edition_title")
    String editionTitle;

    @Column(name = "edition_number")
    Integer editionNumber;

    @Column(name = "edition_issn", unique = false)
    String editionISSN;

    @OneToMany(fetch = FetchType.LAZY)
    Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_id")
    ResearchArea researchArea;
}
