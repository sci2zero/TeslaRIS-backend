package rs.teslaris.core.dto.person;

import java.util.ArrayList;
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
public class ImportPersonDTO extends BasicPersonDTO {

    private String placeOfBirth;

    private List<MultilingualContentDTO> addressLine = new ArrayList<>();

    private List<MultilingualContentDTO> addressCity = new ArrayList<>();

    private List<MultilingualContentDTO> biography = new ArrayList<>();

    private List<MultilingualContentDTO> keywords = new ArrayList<>();
}
