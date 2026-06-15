package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "monograph_publications")
@SQLRestriction("deleted=false")
public class MonographPublication extends Document implements PrintedPageable {

    @Column(name = "monograph_publication_type")
    private MonographPublicationType monographPublicationType;

    @Column(name = "start_page")
    private String startPage;

    @Column(name = "end_page")
    private String endPage;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "article_number")
    private String articleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monograph_id", nullable = false)
    private Monograph monograph;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> section = new HashSet<>();


    public MonographPublication() {
        super(DocumentPublicationType.MONOGRAPH_PUBLICATION);
    }
}
