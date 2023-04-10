package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "journal_publications")
public class JournalPublication extends Document {

    @Column(name = "start_page", nullable = false)
    String startPage;

    @Column(name = "end_page", nullable = false)
    String endPage;

    @Column(name = "number_of_pages", nullable = false)
    Integer numberOfPages;

    @Column(name = "article_number", nullable = false)
    String articleNumber;

    @Column(name = "volume", nullable = false)
    String volume;

    @Column(name = "issue", nullable = false)
    String issue;
    Journal journal;
}
