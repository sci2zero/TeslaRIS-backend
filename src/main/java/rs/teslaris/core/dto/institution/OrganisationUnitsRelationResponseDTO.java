package rs.teslaris.core.dto.institution;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitsRelationResponseDTO {

    private Integer id;

    private List<MultilingualContentDTO> sourceAffiliationStatement;

    private List<MultilingualContentDTO> targetAffiliationStatement;

    private OrganisationUnitRelationType relationType;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private List<DocumentFileResponseDTO> proofs;

    private List<MultilingualContentDTO> sourceOrganisationUnitName;

    private Integer sourceOrganisationUnitId;

    private List<MultilingualContentDTO> targetOrganisationUnitName;

    private Integer targetOrganisationUnitId;
}
