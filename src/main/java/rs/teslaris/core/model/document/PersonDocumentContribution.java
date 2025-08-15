package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(
    name = "person_document_contribution",
    indexes = {
        @Index(name = "idx_pdc_docid_contribtype", columnList = "document_id, contribution_type")
    }
)
@SQLRestriction("deleted=false")
public class PersonDocumentContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    private DocumentContributionType contributionType;

    @Column(name = "main_contributor", nullable = false)
    private Boolean isMainContributor;

    @Column(name = "corresponding_contributor", nullable = false)
    private Boolean isCorrespondingContributor;

    @Column(name = "board_president", nullable = false)
    private Boolean isBoardPresident = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "employment_title")
    private EmploymentTitle employmentTitle;

    @Column(name = "personal_title")
    private PersonalTitle personalTitle;
}
