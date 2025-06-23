package rs.teslaris.core.model.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.util.search.StringUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "person_names")
@SQLRestriction("deleted=false")
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

    @Override
    public String toString() {
        if (Objects.isNull(otherName) || otherName.isEmpty()) {
            return MessageFormat.format("{0} {1}", firstname, lastname);
        }

        return MessageFormat.format("{0} {1} {2}", firstname, otherName, lastname);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PersonName that = (PersonName) o;

        String name = StringUtil.performSimpleSerbianPreprocessing(firstname), surname =
            StringUtil.performSimpleSerbianPreprocessing(lastname);
        String thatName = StringUtil.performSimpleSerbianPreprocessing(that.firstname),
            thatSurname = StringUtil.performSimpleSerbianPreprocessing(that.lastname);

        return (Objects.equals(name, thatName) && Objects.equals(surname, thatSurname)) ||
            (Objects.equals(surname, thatName) && Objects.equals(name, thatSurname));
    }
}
