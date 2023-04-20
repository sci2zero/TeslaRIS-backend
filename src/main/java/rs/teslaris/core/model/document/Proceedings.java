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
    private String eISBN;

    @Column(name = "print_isbn", unique = true)
    private String printISBN;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "edition_title")
    private String editionTitle;

    @Column(name = "edition_number")
    private Integer editionNumber;

    @Column(name = "edition_issn")
    private String editionISSN;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    @Column(name = "journal_volume")
    private String journalVolume;

    @Column(name = "journal_issue")
    private String journalIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;
}
