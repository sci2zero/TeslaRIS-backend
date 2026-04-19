package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.OtherEventType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtherEventDTO extends EventDTO {

    @NotNull(message = "You have to provide event type.")
    private OtherEventType type;
}
