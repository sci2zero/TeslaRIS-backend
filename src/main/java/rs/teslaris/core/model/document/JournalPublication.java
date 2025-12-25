package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "journal_publications")
@SQLRestriction("deleted=false")
public class JournalPublication extends Document implements PrintedPageable {

    @Column(name = "journal_publication_type")
    private JournalPublicationType journalPublicationType;

    @Column(name = "start_page")
    private String startPage;

    @Column(name = "end_page")
    private String endPage;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "article_number")
    private String articleNumber;

    @Column(name = "volume")
    private String volume;

    @Column(name = "issue")
    private String issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;
}
