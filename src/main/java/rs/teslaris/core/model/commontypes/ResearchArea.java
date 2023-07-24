package rs.teslaris.core.model.commontypes;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "research_area")
@Where(clause = "deleted=false")
public class ResearchArea extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "super_research_area_id")
    private ResearchArea superResearchArea;
}
