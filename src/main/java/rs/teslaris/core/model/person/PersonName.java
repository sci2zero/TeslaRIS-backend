package rs.teslaris.core.model.person;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "person_names")
@Where(clause = "deleted=false")
public class PersonName extends BaseEntity {

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "other_name")
    private String otherName;

    @Column(name = "last_name")
    private String lastname;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;
}
