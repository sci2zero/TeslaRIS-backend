package rs.teslaris.core.model.project;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "funding_proposals")
@SQLRestriction("deleted=false")
public class FundingProposal extends BaseEntity {

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> documents = new HashSet<>(); // proposal, review result, supplement

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_call_id", nullable = false)
    private FundingCall fundingCall;

    @Embedded
    private MonetaryAmount requestedBudget;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> responseSummary = new HashSet<>();

    @Column(name = "submission_date")
    private LocalDate submissionDate;

    @Column(name = "review_start_date")
    private LocalDate reviewStartDate;

    @Column(name = "review_end_date")
    private LocalDate reviewEndDate;

    @Column(name = "decision_date")
    private LocalDate decisionDate;

    @Column(name = "revised_proposal_or_next_round_deadline_date")
    private LocalDate revisedProposalOrNextRoundDeadlineDate;

    @Column(name = "proposal_result")
    private ProjectProposalResult proposalResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_proposal_id")
    private FundingProposal revisedProposalOrNextRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id")
    private Funding funding; // in the case it is awarded for funding - ProjectProposalResult
}
