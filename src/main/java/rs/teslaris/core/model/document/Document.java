package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
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
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Document extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> title;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> subTitle;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> description;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> note;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    Set<PersonDocumentContribution> contributors;

    @ElementCollection
    Set<String> uris;

    @Column(name = "document_date", nullable = false)
    String documentDate;

    @OneToMany(fetch = FetchType.LAZY)
    Set<DocumentFile> fileItems;

    @OneToMany(fetch = FetchType.LAZY)
    Set<DocumentFile> proof;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> keywords;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;

    @Column(name = "admin_note", nullable = false)
    String adminNote;

    @Column(name = "doi", nullable = false, unique = true)
    String doi;

    @Column(name = "scopus_id", nullable = false, unique = true)
    String scopusId;

//    @OneToMany(fetch = FetchType.LAZY)
//    Set<PersonDocumentContribution> personDocumentContributions;
}
