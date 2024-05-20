package rs.teslaris.core.dto.document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalBasicAdditionDTO {

    private Integer id;

    @Valid
    @NotNull(message = "You have to provide journal title.")
    private List<MultilingualContentDTO> title;

    private String eISSN;

    private String printISSN;
}
