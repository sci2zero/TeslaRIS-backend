package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonNameDTO {

    @NotBlank(message = "You must provide a first name.")
    private String firstname;

    @NotBlank(message = "You must provide an other name.")
    private String otherName;

    @NotBlank(message = "You must provide a last name.")
    private String lastname;

    @NotNull(message = "You must provide a date of acquisition.")
    private LocalDate dateFrom;

    private LocalDate dateTo;
}
