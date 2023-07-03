package rs.teslaris.core.dto.document;

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
public class ProceedingsResponseDTO extends ProceedingsDTO {

    private List<MultilingualContentDTO> eventName;

    private List<MultilingualContentDTO> publisherName;
}
