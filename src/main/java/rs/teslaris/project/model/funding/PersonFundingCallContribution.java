package rs.teslaris.project.model.funding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.document.PersonContribution;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "person_funding_call_contributions")
@SQLRestriction("deleted=false")
public class PersonFundingCallContribution extends PersonContribution {

    @Column(name = "contribution_type")
    private FundingCallContributionType contributionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_call_id")
    private FundingCall fundingCall;
}
