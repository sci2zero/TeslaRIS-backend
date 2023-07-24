package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "encyclopedic_contributions")
@Where(clause = "deleted=false")
public class EncyclopedicContribution extends Document {
}
