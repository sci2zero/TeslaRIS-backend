package rs.teslaris.core.model.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
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

    @Column(name = "name_type")
    private PersonNameType nameType;

    // A part that does not exist is not in the name, and it does not leave a space or "null".
    private static String joinParts(String... parts) {
        return Arrays.stream(parts).filter(StringUtil::valueExists).map(String::trim)
            .collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return joinParts(firstname, otherName, lastname);
    }

    public String toText() {
        return joinParts(firstname,
            StringUtil.valueExists(otherName) ? "(" + otherName.trim() + ")" : null, lastname);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PersonName that)) {
            return false;
        }

        String name = StringUtil.performSimpleLatinPreprocessing(firstname);
        String surname = StringUtil.performSimpleLatinPreprocessing(lastname);
        String middleName = StringUtil.performSimpleLatinPreprocessing(otherName);

        String thatName = StringUtil.performSimpleLatinPreprocessing(that.firstname);
        String thatSurname = StringUtil.performSimpleLatinPreprocessing(that.lastname);
        String thatMiddleName = StringUtil.performSimpleLatinPreprocessing(that.otherName);

        return (Objects.equals(name, thatName) && Objects.equals(surname, thatSurname) &&
            Objects.equals(middleName, thatMiddleName)) ||
            (Objects.equals(name, thatSurname) && Objects.equals(surname, thatName) &&
                Objects.equals(middleName, thatMiddleName));
    }

    @Override
    public int hashCode() {
        String name = StringUtil.performSimpleLatinPreprocessing(firstname);
        String surname = StringUtil.performSimpleLatinPreprocessing(lastname);
        String middleName = StringUtil.performSimpleLatinPreprocessing(otherName);

        return Objects.hash(
            name, surname, middleName
        ) + Objects.hash(
            surname, name, middleName
        );
    }
}
