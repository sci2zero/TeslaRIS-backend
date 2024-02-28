package rs.teslaris.core.dto.commontypes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryDTO {

    private Integer id;

    private String code;

    private List<MultilingualContentDTO> name;
}
