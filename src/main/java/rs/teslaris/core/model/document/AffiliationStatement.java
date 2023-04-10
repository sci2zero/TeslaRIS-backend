package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;

import java.util.Set;

public class AffiliationStatement {
    Set<MultiLingualContent> displayAffiliationStatement;
    PersonName displayPersonName;
    PostalAddress address;
    Contact contact;
}
