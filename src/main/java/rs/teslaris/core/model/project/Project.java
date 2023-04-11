package rs.teslaris.core.model.project;

import java.time.LocalDate;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> name;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> description;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> nameAbbreviation;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> keywords;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> note;

    @ElementCollection
    Set<String> uris;

    @OneToMany(fetch = FetchType.LAZY)
    Set<PersonProjectContribution> contributions;

    @OneToMany(fetch = FetchType.LAZY)
    Set<ProjectDocument> documents;

    @Column(name = "date_from", nullable = false)
    LocalDate from;

    @Column(name = "date_to", nullable = false)
    LocalDate to;

    @ElementCollection
    Set<ProjectStatus> statuses;

    @Column(name = "type", nullable = false)
    ProjectType type;

    @OneToMany(fetch = FetchType.LAZY)
    Set<Funding> fundings;
}
