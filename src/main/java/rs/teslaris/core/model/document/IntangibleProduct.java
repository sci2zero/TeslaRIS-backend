package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "intangible_products")
@SQLRestriction("deleted=false")
public non-sealed class IntangibleProduct extends Document implements PublisherPublishable {

    @Column(name = "internal_number")
    private String internalNumber;

    @Column(name = "type", nullable = false)
    private IntangibleProductType intangibleProductType;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> productUsers = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Column(name = "author_reprint")
    private Boolean authorReprint = false;
}
