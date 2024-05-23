package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "person_document_contributions")
@SQLRestriction("deleted=false")
public class PersonDocumentContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    private DocumentContributionType contributionType;

    @Column(name = "main_contributor", nullable = false)
    private Boolean isMainContributor;

    @Column(name = "corresponding_contributor", nullable = false)
    private Boolean isCorrespondingContributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
}
