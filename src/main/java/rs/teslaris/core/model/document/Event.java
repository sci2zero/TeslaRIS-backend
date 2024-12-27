package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "events")
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Event extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> nameAbbreviation = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

    @Column(name = "serial_event")
    private Boolean serialEvent = false;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id")
    private Country country;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> place = new HashSet<>();

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonEventContribution> contributions = new HashSet<>();

    @Column(name = "cris_uns_id")
    private Integer oldId;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> uris = new HashSet<>();


    public void addContribution(PersonEventContribution contribution) {
        contributions.add(contribution);
        contribution.setEvent(this);
    }

    public void removeContribution(PersonEventContribution contribution) {
        contribution.setEvent(null);
        contributions.remove(contribution);
    }
}
