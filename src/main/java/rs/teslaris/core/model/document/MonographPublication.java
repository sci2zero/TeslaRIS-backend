package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "monograph_publications")
public class MonographPublication extends Document {

    @Column(name = "start_page", nullable = false)
    String startPage;

    @Column(name = "end_page", nullable = false)
    String endPage;

    @Column(name = "number_of_pages", nullable = false)
    Integer numberOfPages;

    @Column(name = "article_number", nullable = false)
    String articleNumber;
    Monograph monograph;
    Publisher publisher;
}
