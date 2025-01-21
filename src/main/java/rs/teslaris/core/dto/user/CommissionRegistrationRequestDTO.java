package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRegistrationRequestDTO extends EmployeeRegistrationRequestDTO {

    @NotNull(message = "Commission ID cannot be null.")
    @Positive(message = "Commission ID must be a positive number.")
    private Integer commissionId;
}
