package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "publishers")
public class Publisher extends BaseEntity {
    Set<MultiLingualContent> name;
    Set<MultiLingualContent> place;
    Set<MultiLingualContent> state;
}
