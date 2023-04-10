package rs.teslaris.core.model.document;

import java.time.LocalDate;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "events")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Event extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> name;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> nameAbbreviation;

    @Column(name = "from", nullable = false)
    LocalDate from;

    @Column(name = "to", nullable = false)
    LocalDate to;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> state;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> place;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    Set<PersonEventContribution> contributors;

//    @OneToMany(fetch = FetchType.LAZY)
//    Set<PersonEventContribution> personEventContribution; // JEL OVO DUPLIKAT??
}
