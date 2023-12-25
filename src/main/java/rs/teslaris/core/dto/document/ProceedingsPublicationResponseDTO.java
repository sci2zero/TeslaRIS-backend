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
public class ProceedingsPublicationResponseDTO {

    private List<MultilingualContentDTO> proceedingsTitle;

    private List<MultilingualContentDTO> title;

    private String documentDate;
}
