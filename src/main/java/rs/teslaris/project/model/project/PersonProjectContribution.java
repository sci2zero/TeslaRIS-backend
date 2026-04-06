package rs.teslaris.project.model.project;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.project.model.funding.FundingPart;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "person_project_contribution")
@SQLRestriction("deleted=false")
public class PersonProjectContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    private PersonProjectContributionType contributionType;

    @Column(name = "investigation_role", nullable = false)
    private PersonProjectInvestigationRole investigationRole;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> otherRoleDescription = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<FundingPart> fundingParts = new HashSet<>();
    // must be funding allocated only to this person
}
