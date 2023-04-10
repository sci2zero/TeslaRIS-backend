package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scientific_critics")
public class ScientificCritic extends Document {
    Journal journal;
    Monograph monograph;
    Proceedings proceedings;
}
