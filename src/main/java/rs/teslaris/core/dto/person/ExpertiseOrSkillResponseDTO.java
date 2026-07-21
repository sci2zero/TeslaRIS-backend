package rs.teslaris.core.dto.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertiseOrSkillResponseDTO extends ExpertiseOrSkillDTO {

    private List<DocumentFileResponseDTO> proofs;

    private List<ResearchAreaHierarchyDTO> researchAreas = new ArrayList<>();


    public ExpertiseOrSkillResponseDTO(Integer id,
                                       List<MultilingualContentDTO> name,
                                       List<MultilingualContentDTO> description,
                                       List<MultilingualContentDTO> keywords,
                                       Set<Integer> researchAreasId,
                                       Boolean favorite,
                                       List<DocumentFileResponseDTO> proofs,
                                       List<ResearchAreaHierarchyDTO> researchAreas) {
        super(id, name, description, keywords, researchAreasId, favorite);
        this.proofs = proofs;
        this.researchAreas = researchAreas != null ? researchAreas : new ArrayList<>();
    }

    public ExpertiseOrSkillResponseDTO(ExpertiseOrSkillResponseDTO other) {
        super(
            other.getId(),
            Objects.nonNull(other.getName())
                ? other.getName().stream().map(MultilingualContentDTO::new)
                .collect(Collectors.toList())
                : null,
            Objects.nonNull(other.getDescription())
                ? other.getDescription().stream().map(MultilingualContentDTO::new)
                .collect(Collectors.toList())
                : null,
            Objects.nonNull(other.getKeywords())
                ? other.getKeywords().stream().map(MultilingualContentDTO::new)
                .collect(Collectors.toList())
                : null,
            Objects.nonNull(other.getResearchAreasId())
                ? new HashSet<>(other.getResearchAreasId())
                : null,
            other.getFavorite()
        );

        this.proofs = Objects.nonNull(other.getProofs())
            ? other.getProofs().stream().map(DocumentFileResponseDTO::new)
            .collect(Collectors.toList())
            : null;
        this.researchAreas = Objects.nonNull(other.getResearchAreas())
            ? other.getResearchAreas().stream().map(ResearchAreaHierarchyDTO::new)
            .collect(Collectors.toList())
            : new ArrayList<>();
    }
}
