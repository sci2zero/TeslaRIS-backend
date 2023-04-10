package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "affiliation_statements")
public class AffiliationStatement extends BaseEntity {
    Set<MultiLingualContent> displayAffiliationStatement;
    PersonName displayPersonName;
    PostalAddress address;
    Contact contact;
}
