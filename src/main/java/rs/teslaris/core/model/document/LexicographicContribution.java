package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lexicographic_contributions")
public class LexicographicContribution extends Document {
}
