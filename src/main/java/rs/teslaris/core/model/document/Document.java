package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
@Where(clause = "deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Document extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> title;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> subTitle;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonDocumentContribution> contributors;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> uris;

    @Column(name = "document_date")
    private String documentDate;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> fileItems;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> proofs;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @Column(name = "doi")
    private String doi;

    @Column(name = "scopus_id")
    private String scopusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "cris_uns_id")
    private Integer oldId;


    public void addDocumentContribution(PersonDocumentContribution contribution) {
        contribution.setDocument(this);
        contributors.add(contribution);
    }

    public void removeDocumentContribution(PersonDocumentContribution contribution) {
        contribution.setDocument(null);
        contributors.remove(contribution);
    }
}
