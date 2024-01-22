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
public class PublisherDTO {

    private Integer id;

    @Valid
    @NotNull(message = "You have to provide publisher name.")
    private List<MultilingualContentDTO> name;

    @NotNull(message = "You have to provide publisher place.")
    private List<MultilingualContentDTO> place;

    @NotNull(message = "You have to provide publisher state.")
    private List<MultilingualContentDTO> state;
}
