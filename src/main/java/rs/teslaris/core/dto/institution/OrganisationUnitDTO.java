package rs.teslaris.core.dto.institution;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitDTO extends OrganisationUnitRequestDTO {

    private Integer id;

    private Set<ResearchAreaHierarchyDTO> researchAreas = new HashSet<>();

    private String logoServerFilename;

    private String logoBackgroundHex;

    private Integer superInstitutionId;

    private List<MultilingualContentDTO> superInstitutionName;
}
