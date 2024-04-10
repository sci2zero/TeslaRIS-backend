package rs.teslaris.core.model.project;

import java.time.LocalDate;
import java.util.HashSet;
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
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects")
@Where(clause = "deleted=false")
public class Project extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> nameAbbreviation = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

    @ElementCollection
    private Set<String> uris = new HashSet<>();

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonProjectContribution> contributions = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ProjectDocument> documents = new HashSet<>();

    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @ElementCollection
    private Set<ProjectStatus> statuses = new HashSet<>();

    @Column(name = "type", nullable = false)
    private ProjectType type;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Funding> fundings = new HashSet<>();
}
