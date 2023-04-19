package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "person_document_contributions")
public class PersonDocumentContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    private DocumentContributionType contributionType;

    @Column(name = "main_contributor", nullable = false)
    private boolean mainContributor;

    @Column(name = "corresponding_contributor", nullable = false)
    private boolean correspondingContributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
}
