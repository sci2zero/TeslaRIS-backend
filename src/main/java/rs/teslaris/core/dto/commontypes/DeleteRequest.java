package rs.teslaris.core.dto.commontypes;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteRequest {

    @NotNull(message = "You have to provide a list of IDs.")
    private List<Integer> ids;
}
