package rs.teslaris.core.dto.person.involvement;

import java.time.LocalDate;
import rs.teslaris.core.model.person.EmploymentPosition;

public record EmploymentMigrationDTO(
    Integer personOldId,
    Integer chairOldId,
    EmploymentPosition employmentPosition,
    LocalDate employmentStartDate,
    String personAccountingId,
    String chairAccountingId
) {
}
