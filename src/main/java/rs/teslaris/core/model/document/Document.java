package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.util.deduplication.Mergeable;

@Getter
@Setter
@AllArgsConstructor
@Entity
@DynamicUpdate
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_doi", columnList = "doi"),
    @Index(name = "idx_document_scopus", columnList = "scopus_id"),
    @Index(name = "idx_document_open_alex", columnList = "open_alex_id")
})
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Document extends BaseEntity implements Mergeable {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> title = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> subTitle = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> remark = new HashSet<>();

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private Set<PersonDocumentContribution> contributors = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @Column(name = "document_date")
    private String documentDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private Set<DocumentFile> fileItems = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private Set<DocumentFile> proofs = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @Column(name = "doi")
    private String doi;

    @Column(name = "scopus_id")
    private String scopusId;

    @Column(name = "open_alex_id")
    private String openAlexId;

    @Column(name = "web_of_science_id")
    private String webOfScienceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private Event event;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "old_ids")
    private Set<Integer> oldIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "merged_ids")
    private Set<Integer> mergedIds = new HashSet<>();

    @Column(name = "is_metadata_valid")
    private Boolean isMetadataValid = true;

    @Column(name = "are_files_valid")
    private Boolean areFilesValid = true;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "internal_identifiers")
    private Set<String> internalIdentifiers = new HashSet<>();

    @Column(name = "document_type", nullable = false)
    private DocumentPublicationType documentType;


    protected Document(DocumentPublicationType documentType) {
        this.documentType = documentType;
    }

    protected Document() {
        // for JPA
    }

    protected Document(Document other) {
        this.documentDate = other.documentDate;
        this.approveStatus = other.approveStatus;
        this.doi = other.doi;
        this.scopusId = other.scopusId;
        this.openAlexId = other.openAlexId;
        this.webOfScienceId = other.webOfScienceId;
        this.isMetadataValid = other.isMetadataValid;
        this.areFilesValid = other.areFilesValid;
        this.isArchived = other.isArchived;

        this.title = other.title.stream()
            .map(mt -> new MultiLingualContent(mt.getLanguage(), mt.getContent(), mt.getPriority()))
            .collect(Collectors.toCollection(HashSet::new));
        this.subTitle = other.subTitle.stream()
            .map(mt -> new MultiLingualContent(mt.getLanguage(), mt.getContent(), mt.getPriority()))
            .collect(Collectors.toCollection(HashSet::new));
        this.description = other.description.stream()
            .map(mt -> new MultiLingualContent(mt.getLanguage(), mt.getContent(), mt.getPriority()))
            .collect(Collectors.toCollection(HashSet::new));
        this.remark = other.remark.stream()
            .map(mt -> new MultiLingualContent(mt.getLanguage(), mt.getContent(), mt.getPriority()))
            .collect(Collectors.toCollection(HashSet::new));
        this.keywords = other.keywords.stream()
            .map(mt -> new MultiLingualContent(mt.getLanguage(), mt.getContent(), mt.getPriority()))
            .collect(Collectors.toCollection(HashSet::new));

        this.contributors = other.contributors.stream()
            .map(c -> new PersonDocumentContribution(c, this))
            .collect(Collectors.toCollection(HashSet::new));
        other.contributors.clear();

        this.fileItems = other.fileItems.stream()
            .peek(f -> f.setDocument(this))
            .collect(Collectors.toCollection(HashSet::new));
        other.fileItems.clear();

        this.proofs = other.proofs.stream()
            .peek(f -> f.setDocument(this))
            .collect(Collectors.toCollection(HashSet::new));
        other.proofs.clear();

        this.uris = new HashSet<>(other.uris);
        other.uris.clear();
        this.oldIds = new HashSet<>(other.oldIds);
        other.oldIds.clear();
        this.mergedIds = new HashSet<>(other.mergedIds);
        other.mergedIds.clear();
        this.internalIdentifiers = new HashSet<>(other.internalIdentifiers);
        other.internalIdentifiers.clear();
    }

    public void addDocumentContribution(PersonDocumentContribution contribution) {
        contribution.setDocument(this);
        contributors.add(contribution);
    }

    public void removeDocumentContribution(PersonDocumentContribution contribution) {
        contribution.setDocument(null);
        contributors.remove(contribution);
    }
}
