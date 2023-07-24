package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "informative_attachments")
@Where(clause = "deleted=false")
public class InformativeAttachment extends Document {
}
