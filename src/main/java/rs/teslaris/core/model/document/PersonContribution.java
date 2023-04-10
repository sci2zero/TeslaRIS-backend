package rs.teslaris.core.model.document;

import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;

import java.util.Set;

public class PersonContribution {
    Person person;
    Set<MultiLingualContent> contributionDescription;
    AffiliationStatement affiliationStatement;
    Set<OrganisationUnit> institutions;
    int orderNumber;
    ApproveStatus approveStatus;
}
