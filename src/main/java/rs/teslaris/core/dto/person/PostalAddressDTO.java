package rs.teslaris.core.dto.person;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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

    @Valid
    @NotNull(message = "You have to provide a state.")
    private List<MultilingualContentDTO> state;

    private String postalNumber;


    public PostalAddressDTO(PostalAddressDTO other) {
        this.countryId = other.countryId;
        this.streetAndNumber = Objects.nonNull(other.streetAndNumber)
            ? other.streetAndNumber.stream().map(MultilingualContentDTO::new)
            .collect(Collectors.toList())
            : null;
        this.city = Objects.nonNull(other.city)
            ? other.city.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.state = Objects.nonNull(other.state)
            ? other.state.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.postalNumber = other.postalNumber;
    }
}
