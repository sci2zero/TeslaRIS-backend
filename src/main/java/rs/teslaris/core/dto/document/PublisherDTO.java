package rs.teslaris.core.dto.document;

import java.util.List;
import javax.validation.Valid;
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

    private Integer id;

    @Valid
    private List<MultilingualContentDTO> name;

    @Valid
    private List<MultilingualContentDTO> place;

    @Valid
    private List<MultilingualContentDTO> state;
}
