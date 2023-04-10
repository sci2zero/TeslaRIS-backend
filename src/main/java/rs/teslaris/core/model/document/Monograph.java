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

    @Column(name = "print_isbn", nullable = false, unique = true)
    String printISBN;

    @Column(name = "e_isbn", nullable = false, unique = true)
    String eISBN;

    @Column(name = "number_of_pages", nullable = false)
    Integer numberOfPages;

    @Column(name = "edition_title", nullable = false)
    String editionTitle;

    @Column(name = "edition_number", nullable = false)
    Integer editionNumber;

    @Column(name = "edition_issn", nullable = false, unique = true)
    String editionISSN;

    @OneToMany(fetch = FetchType.EAGER)
    Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_id")
    ResearchArea researchArea;
}
