package rs.teslaris.project.model.project;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.OrganisationUnitContribution;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "organisation_unit_project_contributions")
@SQLRestriction("deleted=false")
public class OrganisationUnitProjectContribution extends OrganisationUnitContribution {

    @Column(name = "contribution_type")
    private OrganisationUnitProjectContributionType contributionType;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> displayProject = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
