package rs.teslaris.core.dto.person.involvement;

import java.time.LocalDate;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.EmploymentPosition;

public record ExtraEmploymentMigrationDTO(
    Integer personAccountingId,
    EmploymentPosition employmentPosition,
    LocalDate employmentStartDate,
    String organisationUnitName,
    PersonNameDTO personName
) {
}
