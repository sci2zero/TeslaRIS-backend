package rs.teslaris.core.model.document;

import java.time.LocalDate;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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
    Set<MultiLingualContent> name;
    Set<MultiLingualContent> nameAbbreviation;

    @Column(name = "from", nullable = false)
    LocalDate from;

    @Column(name = "to", nullable = false)
    LocalDate to;
    Set<MultiLingualContent> state;
    Set<MultiLingualContent> place;
    Set<PersonEventContribution> contributors;

    Set<PersonEventContribution> personEventContribution;
}
