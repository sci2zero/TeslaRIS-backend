package rs.teslaris.core.dto.person;

import java.time.LocalDate;
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

    public PrizeResponseDTO(PrizeResponseDTO other) {
        super(
            Objects.nonNull(other.getTitle())
                ? other.getTitle().stream().map(MultilingualContentDTO::new)
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
            other.getDate(),
            other.getEndDate(),
            other.getPrizeType(),
            other.getFavorite(),
            Objects.nonNull(other.getResearchAreasId()) ?
                new HashSet<>(other.getResearchAreasId()) : null
        );
        this.id = other.id;
        this.proofs = Objects.nonNull(other.proofs)
            ? other.proofs.stream().map(DocumentFileResponseDTO::new).collect(Collectors.toList())
            : null;
        this.researchAreas = Objects.nonNull(other.researchAreas)
            ? other.researchAreas.stream().map(ResearchAreaHierarchyDTO::new)
            .collect(Collectors.toList())
            : new ArrayList<>();
    }
}
