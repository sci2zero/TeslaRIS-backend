package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
