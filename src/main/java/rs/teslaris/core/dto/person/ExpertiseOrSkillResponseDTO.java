package rs.teslaris.core.dto.person;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertiseOrSkillResponseDTO {

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description;

    private List<DocumentFileResponseDTO> documentFiles;
}
