package rs.teslaris.core.dto.identifier;

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
public class PersonIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide person ID.")
    @Positive(message = "Person ID must be a positive number.")
    private Integer personId;


    public PersonIdentifierDTO(String value, Integer identifierId, Integer personId) {
        super(value, identifierId);
        this.personId = personId;
    }
}
