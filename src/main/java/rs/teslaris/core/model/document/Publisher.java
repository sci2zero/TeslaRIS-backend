package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "publishers")
@Where(clause = "deleted=false")
public class Publisher extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> place;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> state;
}
