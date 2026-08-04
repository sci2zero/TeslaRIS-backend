package rs.teslaris.project.model.project;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

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
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_project_id")
    private Project sourceProject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_project_id")
    private Project targetProject;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> sourceProjectDescription = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> targetProjectDescription = new HashSet<>();

}
