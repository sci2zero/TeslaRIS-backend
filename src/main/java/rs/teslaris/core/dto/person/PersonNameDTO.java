package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.NotBlank;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.person.PersonNameType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonNameDTO {

    private Integer id;

    @NotBlank(message = "You must provide a first name.")
    private String firstname;

    private String otherName;

    @NotBlank(message = "You must provide a last name.")
    private String lastname;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private PersonNameType personNameType;


    public PersonNameDTO(PersonNameDTO other) {
        this.id = other.id;
        this.firstname = other.firstname;
        this.otherName = other.otherName;
        this.lastname = other.lastname;
        this.dateFrom = other.dateFrom;
        this.dateTo = other.dateTo;
        this.personNameType = other.personNameType;
    }

    @Override
    public String toString() {
        if (Objects.isNull(otherName) || otherName.isEmpty()) {
            return MessageFormat.format("{0} {1}", firstname, lastname);
        }

        return MessageFormat.format("{0} {1} {2}", firstname, otherName, lastname);
    }
}
