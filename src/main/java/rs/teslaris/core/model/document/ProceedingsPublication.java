package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "proceedings_publications")
@Where(clause = "deleted=false")
public class ProceedingsPublication extends Document {

    @Column(name = "proceedings_publication_type", nullable = false)
    @ColumnDefault("0")
    private ProceedingsPublicationType proceedingsPublicationType;

    @Column(name = "start_page")
    private String startPage;

    @Column(name = "end_page")
    private String endPage;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "article_number")
    private String articleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceedings_id", nullable = false)
    private Proceedings proceedings;
}
