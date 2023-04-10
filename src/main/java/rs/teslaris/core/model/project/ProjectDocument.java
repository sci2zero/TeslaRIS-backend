package rs.teslaris.core.model.project;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_documents")
public class ProjectDocument extends BaseEntity {
//    Document document;  // PRIVREMENO ZAKOMENTARISANO DA VIDIMO RADI LI PODSKUP

    @Column(name = "relation_type", nullable = false)
    ProjectDocumentType relationType;
}
