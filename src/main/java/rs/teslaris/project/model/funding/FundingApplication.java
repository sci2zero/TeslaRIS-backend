package rs.teslaris.project.model.funding;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectProposalResult;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "funding_applications")
@SQLRestriction("deleted=false")
public class FundingApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_call_id")
    private FundingCall fundingCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_application_id")
    private FundingApplication fundingApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id")
    private Funding funding;

    @Embedded
    @AttributeOverride(
        name = "amount", column = @Column(name = "requested_amount"))
    @AssociationOverride(
        name = "currency", joinColumns = @JoinColumn(name = "requested_currency_id"))
    private MonetaryAmount requestedAmount;

    @Embedded
    @AttributeOverride(
        name = "amount", column = @Column(name = "other_funding_amount"))
    @AssociationOverride(
        name = "currency", joinColumns = @JoinColumn(name = "pother_funding_currency_id"))
    private MonetaryAmount otherFundingSourceAmount;

    @Embedded
    @AttributeOverride(
        name = "amount", column = @Column(name = "other_source_amount"))
    @AssociationOverride(
        name = "currency", joinColumns = @JoinColumn(name = "other_source_currency_id"))
    private MonetaryAmount otherSourceFunding;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> responseSummary = new HashSet<>();

    @Column(name = "submission_date")
    private LocalDate submissionDate;

    @Column(name = "review_date_from")
    private LocalDate reviewDateFrom;

    @Column(name = "review_date_to")
    private LocalDate reviewDateTo;

    @Column(name = "decision_date")
    private LocalDate decisionDate;

    @Column(name = "revised_proposal_date")
    private LocalDate revisedProposalOrNextRoundDeadlineDate;

    @Column(name = "result")
    private ProjectProposalResult result;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> documents = new HashSet<>();
}
