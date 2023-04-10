package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "person_event_contributions")
public class PersonEventContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    EventContributionType contributionType;
}
