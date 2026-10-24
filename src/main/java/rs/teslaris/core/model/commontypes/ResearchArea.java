package rs.teslaris.core.model.commontypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "research_area")
@SQLRestriction("deleted=false")
public class ResearchArea extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "super_research_area_id")
    private ResearchArea superResearchArea;

    @Column(name = "processed_name", length = 2048)
    private String processedName;

    @Column(name = "scheme_name")
    private String schemeName;


    // Identity is the primary key, never the content. Hibernate keys the snapshot of a collection
    // holding research areas by the elements as they hashed when it loaded, so a hash derived from
    // content that is still being resolved at that point goes stale and an unchanged element is
    // taken for a new one, which duplicates its row in the join table.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResearchArea other)) {
            return false;
        }
        return Objects.nonNull(getId()) && Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
