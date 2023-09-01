package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "book_series")
@Where(clause = "deleted=false")
public class BookSeries extends PublicationSeries {
}
