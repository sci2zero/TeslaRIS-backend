package rs.teslaris.core.model.person;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "person_names")
public class PersonName extends BaseEntity {

    @Column(name = "firstname", nullable = false)
    String firstname;

    @Column(name = "other_name", nullable = false)
    String otherName;

    @Column(name = "last_name", nullable = false)
    String lastname;

    @Column(name = "date_from", nullable = false)
    LocalDate from;

    @Column(name = "date_to", nullable = false)
    LocalDate to;
}
