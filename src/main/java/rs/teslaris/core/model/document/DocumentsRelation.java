package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents_relations")
@Where(clause = "deleted=false")
public class DocumentsRelation extends BaseEntity {

    @Column(name = "relation_type", nullable = false)
    private DocumentsRelationType relationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private Document sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_document_id")
    private Document targetDocument;
}
