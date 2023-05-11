package rs.teslaris.core.dto.person;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostalAddressDTO {

    @Positive(message = "Country ID must be a positive number")
    @NotNull(message = "You have to provide a country ID.")
    private Integer countryId;

    @Valid
    private List<MultilingualContentDTO> streetAndNumber;

    @Valid
    private List<MultilingualContentDTO> city;
}
