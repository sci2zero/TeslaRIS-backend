package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Arrays;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "proceedings_publications")
@SQLRestriction("deleted=false")
public class ProceedingsPublication extends Document implements PrintedPageable {

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


    public ProceedingsPublication(JournalPublication journalPublication) {
        super(journalPublication);

        this.startPage = journalPublication.getStartPage();
        this.endPage = journalPublication.getEndPage();
        this.numberOfPages = journalPublication.getNumberOfPages();
        this.startPage = journalPublication.getStartPage();
        this.articleNumber = journalPublication.getArticleNumber();
        this.proceedingsPublicationType =
            Arrays.stream(ProceedingsPublicationType.values())
                .filter(target -> target.name()
                    .equals(journalPublication.getJournalPublicationType().name()))
                .findFirst()
                .orElse(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
    }
}
