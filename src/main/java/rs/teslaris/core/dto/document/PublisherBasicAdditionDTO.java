package rs.teslaris.core.dto.document;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PublisherBasicAdditionDTO {

    private Integer id;

    @NotNull(message = "You have to provide publisher name.")
    private List<@Valid MultilingualContentDTO> name;

    @Positive(message = "Country ID must be a positive number")
    private Integer countryId;
}
