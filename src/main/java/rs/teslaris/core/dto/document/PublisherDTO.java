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
public class PublisherDTO {

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> place;

    private List<MultilingualContentDTO> state;
}
