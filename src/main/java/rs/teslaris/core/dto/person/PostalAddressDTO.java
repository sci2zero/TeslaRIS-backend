package rs.teslaris.core.dto.person;

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

    private Integer countryId;

    private List<MultilingualContentDTO> streetAndNumber;

    private List<MultilingualContentDTO> city;
}
