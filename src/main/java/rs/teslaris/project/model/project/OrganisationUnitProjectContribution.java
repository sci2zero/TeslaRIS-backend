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
// Bug case (callSuper=false): same Project has 15 different contributions where 14 of them have
// the same contributionType -> Set treats them as 2 different entities instead of 15
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "organisation_unit_project_contributions")
@SQLRestriction("deleted=false")
public class OrganisationUnitProjectContribution extends OrganisationUnitContribution {

    @Column(name = "contribution_type")
    private OrganisationUnitProjectContributionType contributionType;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> displayProject = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
