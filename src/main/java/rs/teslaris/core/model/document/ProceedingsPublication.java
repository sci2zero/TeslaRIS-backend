package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "proceedings_publications")
public class ProceedingsPublication extends Document {

    @Column(name = "proceedings_publication_type", nullable = false)
    @ColumnDefault("0")
    ProceedingsPublicationType proceedingsPublicationType;

    @Column(name = "start_page")
    String startPage;

    @Column(name = "end_page")
    String endPage;

    @Column(name = "number_of_pages")
    Integer numberOfPages;

    @Column(name = "article_number")
    String articleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceedings_id", nullable = false)
    Proceedings proceedings;
}
