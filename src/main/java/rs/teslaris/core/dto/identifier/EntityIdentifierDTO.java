package rs.teslaris.core.dto.identifier;

import jakarta.validation.constraints.NotBlank;
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
public class EntityIdentifierDTO {

    @NotBlank(message = "You have to provide identifier value.")
    private String value;

    @NotNull(message = "You have to provide identifier ID.")
    @Positive(message = "Identifier ID must be a positive number.")
    private Integer identifierId;
}
