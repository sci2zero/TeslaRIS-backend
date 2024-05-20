package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
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
    private Set<MultiLingualContent> title = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> subTitle = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonDocumentContribution> contributors = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> uris = new HashSet<>();

    @Column(name = "document_date")
    private String documentDate;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> fileItems = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> proofs = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

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
