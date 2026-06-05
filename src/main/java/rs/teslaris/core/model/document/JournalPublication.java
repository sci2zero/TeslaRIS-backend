package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

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

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> section = new HashSet<>();


    public JournalPublication() {
        super(DocumentPublicationType.JOURNAL_PUBLICATION);
    }

    public JournalPublication(ProceedingsPublication proceedingsPublication) {
        super(proceedingsPublication);

        this.startPage = proceedingsPublication.getStartPage();
        this.endPage = proceedingsPublication.getEndPage();
        this.numberOfPages = proceedingsPublication.getNumberOfPages();
        this.startPage = proceedingsPublication.getStartPage();
        this.articleNumber = proceedingsPublication.getArticleNumber();
        this.journalPublicationType = Arrays.stream(JournalPublicationType.values())
            .filter(target -> target.name()
                .equals(proceedingsPublication.getProceedingsPublicationType().name()))
            .findFirst()
            .orElse(JournalPublicationType.RESEARCH_ARTICLE);

        this.section = proceedingsPublication.getSection().stream()
            .map(mt -> new MultiLingualContent(mt.getLanguage(), mt.getContent(), mt.getPriority()))
            .collect(Collectors.toCollection(HashSet::new));

        this.setDocumentType(DocumentPublicationType.JOURNAL_PUBLICATION);
    }
}
