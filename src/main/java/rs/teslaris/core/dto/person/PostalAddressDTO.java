package rs.teslaris.core.dto.person;

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
public class PostalAddressDTO {

    @Positive(message = "Country ID must be a positive number")
    private Integer countryId;

    @Valid
    @NotNull(message = "You have to provide street and number")
    private List<MultilingualContentDTO> streetAndNumber;

    @Valid
    @NotNull(message = "You have to provide a city.")
    private List<MultilingualContentDTO> city;
}
