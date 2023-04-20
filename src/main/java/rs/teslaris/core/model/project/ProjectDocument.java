package rs.teslaris.core.model.project;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_documents")
public class ProjectDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Document document;

    @Column(name = "relation_type", nullable = false)
    private ProjectDocumentType relationType;
}
