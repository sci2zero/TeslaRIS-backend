package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deduplication_suggestions")
@SQLRestriction("deleted=false")
public class DeduplicationSuggestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_document_id", nullable = false)
    private Document leftDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "right_document_id", nullable = false)
    private Document rightDocument;

    @Column(name = "document_type")
    private DocumentPublicationType documentType;
}
