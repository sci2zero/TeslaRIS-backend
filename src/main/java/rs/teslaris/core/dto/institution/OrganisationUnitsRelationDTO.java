package rs.teslaris.core.dto.institution;

import java.time.LocalDate;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitsRelationDTO {
    private Collection<MultilingualContentDTO> sourceAffiliationStatement;

    private Collection<MultilingualContentDTO> targetAffiliationStatement;

    private OrganisationUnitRelationType relationType;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private ApproveStatus approveStatus;

    private Collection<DocumentFileDTO> proofs;

    private OrganisationUnitDTO sourceOrganisationUnit;

    private OrganisationUnitDTO targetOrganisationUnit;
}
