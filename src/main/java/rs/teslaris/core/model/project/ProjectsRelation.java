package rs.teslaris.core.model.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_relations")
@SQLRestriction("deleted=false")
public class ProjectsRelation extends BaseEntity {

    @Column(name = "relation_type", nullable = false)
    private ProjectsRelationType relationType;

    @Column(name = "date_from", nullable = false)
    private LocalDate from;

    @Column(name = "date_to", nullable = false)
    private LocalDate to;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_project_id")
    private Project sourceProject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_project_id")
    private Project targetProject;
}
