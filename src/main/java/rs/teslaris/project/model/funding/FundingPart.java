package rs.teslaris.project.model.funding;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.project.ProjectDocument;
import rs.teslaris.project.model.project.ProjectEvent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "funding_parts",
    indexes = {
        @Index(name = "idx_funding_parts_project_event", columnList = "project_event_id"),
        @Index(name = "idx_funding_parts_proposal", columnList = "funding_proposal_id"),
        @Index(name = "idx_funding_parts_project_document", columnList = "project_document_id"),
        @Index(name = "idx_funding_parts_for_funding", columnList = "for_funding_id"),
        @Index(name = "idx_funding_parts_funding", columnList = "funding_id")
    })
@SQLRestriction("deleted=false")
public class FundingPart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id")
    private Funding funding;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @Embedded
    private MonetaryAmount costs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_event_id")
    private ProjectEvent projectEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_proposal_id")
    private FundingProposal fundingProposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_document_id")
    private ProjectDocument projectDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "for_funding_id")
    private Funding forFunding;
}
