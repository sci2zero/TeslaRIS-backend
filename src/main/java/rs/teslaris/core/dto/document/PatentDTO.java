package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
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
public class PatentDTO extends DocumentDTO implements PublishableDTO {

    private String number;

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;

    // used only for responses

    private List<MultilingualContentDTO> publisherName = new ArrayList<>();
}
