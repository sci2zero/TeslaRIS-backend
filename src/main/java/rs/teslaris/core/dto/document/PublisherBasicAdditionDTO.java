package rs.teslaris.core.dto.document;


import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublisherBasicAdditionDTO {

    private Integer id;

    @NotNull(message = "You have to provide publisher name.")
    private List<@Valid MultilingualContentDTO> name;

    private List<@Valid MultilingualContentDTO> state;
}
