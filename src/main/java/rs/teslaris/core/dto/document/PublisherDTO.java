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
public class PublisherDTO {

    private Integer id;

    @Valid
    @NotNull(message = "You have to provide publisher name.")
    private List<MultilingualContentDTO> name;

    @NotNull(message = "You have to provide publisher place.")
    private List<MultilingualContentDTO> place;

    @Positive(message = "Country ID must be a positive number")
    private Integer countryId;
}
