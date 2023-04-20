package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonNameDTO {

    private String firstname;

    private String otherName;

    private String lastname;

    private LocalDate dateFrom;

    private LocalDate dateTo;
}
