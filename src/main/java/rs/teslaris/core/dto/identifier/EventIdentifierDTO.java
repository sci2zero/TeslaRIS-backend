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
public class EventIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide event ID.")
    @Positive(message = "Event ID must be a positive number.")
    private Integer eventId;


    public EventIdentifierDTO(String value, Integer identifierId, Integer eventId) {
        super(value, identifierId);
        this.eventId = eventId;
    }
}
