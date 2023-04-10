package rs.teslaris.core.dto;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivateAccountRequestDTO {

    @NotBlank(message = "Activation token cannot be blank.")
    private String activationToken;
}
