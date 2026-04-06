package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.person.PrizeType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrizeResponseDTO extends PrizeDTO {

    private Integer id;

    private List<DocumentFileResponseDTO> proofs;

    private List<ResearchAreaHierarchyDTO> researchAreas = new ArrayList<>();


    public PrizeResponseDTO(List<MultilingualContentDTO> title,
                            List<MultilingualContentDTO> description,
                            List<MultilingualContentDTO> keywords, LocalDate date,
                            LocalDate endDate,
                            PrizeType prizeType, Boolean favorite,
                            Set<Integer> researchAreasId, Integer id,
                            List<DocumentFileResponseDTO> proofs,
                            List<ResearchAreaHierarchyDTO> researchAreas) {
        super(title, description, keywords, date, endDate, prizeType, favorite, researchAreasId);
        this.id = id;
        this.proofs = proofs;
        this.researchAreas = researchAreas;
    }
}
