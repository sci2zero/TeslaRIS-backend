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

    @Column(name = "e_isbn", unique = true)
    String eISBN;

    @Column(name = "print_isbn", unique = true)
    String printISBN;

    @Column(name = "number_of_pages")
    Integer numberOfPages;

    @Column(name = "edition_title")
    String editionTitle;

    @Column(name = "edition_number")
    Integer editionNumber;

    @Column(name = "edition_issn")
    String editionISSN;

    @OneToMany(fetch = FetchType.LAZY)
    Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    Journal journal;

    @Column(name = "journal_volume")
    String journalVolume;

    @Column(name = "journal_issue")
    String journalIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    Publisher publisher;
}
