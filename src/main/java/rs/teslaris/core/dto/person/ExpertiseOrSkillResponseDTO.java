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
public class ExpertiseOrSkillResponseDTO extends ExpertiseOrSkillDTO {

    private List<DocumentFileResponseDTO> proofs;

    public ExpertiseOrSkillResponseDTO(Integer id,
                                       List<MultilingualContentDTO> name,
                                       List<MultilingualContentDTO> description,
                                       List<DocumentFileResponseDTO> proofs) {
        super(id, name, description);
        this.proofs = proofs;
    }
}
