package rs.teslaris.core.model.document;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
