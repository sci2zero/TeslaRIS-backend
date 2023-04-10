package rs.teslaris.core.model.project;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.PersonContribution;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "person_project_contribution")
public class PersonProjectContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    ProjectContributionType contributionType;

    @Column(name = "from", nullable = false)
    LocalDate from;

    @Column(name = "to", nullable = false)
    LocalDate to;
}
