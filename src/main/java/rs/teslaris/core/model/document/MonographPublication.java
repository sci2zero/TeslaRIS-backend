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
@Table(name = "monograph_publications")
public class MonographPublication extends Document {

    @Column(name = "monograph_publication_type")
    MonographPublicationType monographPublicationType;

    @Column(name = "start_page")
    String startPage;

    @Column(name = "end_page")
    String endPage;

    @Column(name = "number_of_pages")
    Integer numberOfPages;

    @Column(name = "article_number")
    String articleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monograph_id", nullable = false)
    Monograph monograph;
}
