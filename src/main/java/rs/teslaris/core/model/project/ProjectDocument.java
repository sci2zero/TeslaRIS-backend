package rs.teslaris.core.model.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_documents")
@SQLRestriction("deleted=false")
public class ProjectDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Document document;

    @Column(name = "relation_type", nullable = false)
    private ProjectDocumentType relationType;
}
