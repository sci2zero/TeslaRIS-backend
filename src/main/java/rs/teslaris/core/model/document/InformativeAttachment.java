package rs.teslaris.core.model.document;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "informative_attachments")
@SQLRestriction("deleted=false")
public class InformativeAttachment extends Document {
}
