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

@Getter
@Setter
@Entity
@Table(name = "proceedings")
public class Proceedings extends Document {

    @Column(name = "e_isbn", nullable = false, unique = true)
    String eISBN;

    @Column(name = "print_isbn", nullable = false, unique = true)
    String printISBN;

    @Column(name = "number_of_pages", nullable = false)
    Integer numberOfPages;

    @Column(name = "edition_title", nullable = false)
    String editionTitle;

    @Column(name = "edition_number", nullable = false)
    Integer editionNumber;

    @Column(name = "edition_issn", nullable = false)
    String editionISSN;

    @OneToMany(fetch = FetchType.EAGER)
    Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    Journal journal;

    @Column(name = "journal_volume", nullable = false)
    String journalVolume;

    @Column(name = "journal_issue", nullable = false)
    String journalIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id")
    Conference Conference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    Publisher publisher;
}
