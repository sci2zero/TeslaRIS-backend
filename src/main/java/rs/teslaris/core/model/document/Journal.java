package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "journals")
@Where(clause = "deleted=false")
public class Journal extends PublicationSeries {
}
