package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "person_contributions")
@Inheritance(strategy = InheritanceType.JOINED)
public class PersonContribution extends BaseEntity {
    Person person;
    Set<MultiLingualContent> contributionDescription;
    AffiliationStatement affiliationStatement;
    Set<OrganisationUnit> institutions;

    @Column(name = "order_number", nullable = false)
    int orderNumber;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;
}
