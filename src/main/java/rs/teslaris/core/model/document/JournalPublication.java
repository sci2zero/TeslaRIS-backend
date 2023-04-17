package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "journal_publications")
public class JournalPublication extends Document {

    @Column(name = "journal_publication_type")
    JournalPublicationType journalPublicationType;

    @Column(name = "start_page")
    String startPage;

    @Column(name = "end_page")
    String endPage;

    @Column(name = "number_of_pages")
    Integer numberOfPages;

    @Column(name = "article_number")
    String articleNumber;

    @Column(name = "volume")
    String volume;

    @Column(name = "issue")
    String issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    Journal journal;
}
