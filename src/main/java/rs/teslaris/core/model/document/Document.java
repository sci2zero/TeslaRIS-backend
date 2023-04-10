package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {
    Set<MultiLingualContent> title;
    Set<MultiLingualContent> subTitle;
    Set<MultiLingualContent> description;
    Set<MultiLingualContent> note;
    Set<PersonDocumentContribution> contributors;
    Set<String> uris;

    @Column(name = "document_date", nullable = false)
    String documentDate;
    Set<DocumentFile> fileItems;
    Set<DocumentFile> proof;
    Set<MultiLingualContent> keywords;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;

    @Column(name = "admin_note", nullable = false)
    String adminNote;

    @Column(name = "doi", nullable = false, unique = true)
    String doi;

    @Column(name = "scopus_id", nullable = false, unique = true)
    String scopusId;

    Set<PersonDocumentContribution> personDocumentContributions;
}
