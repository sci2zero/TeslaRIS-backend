package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface OrganisationUnitService extends JPAService<OrganisationUnit> {

    OrganisationUnit findOrganisationUnitById(Integer id);

    OrganisationUnit getReferenceToOrganisationUnitById(Integer id);

    Page<OrganisationUnitDTO> findOrganisationUnits(Pageable pageable);

    OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id);

    Page<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(Integer sourceId,
                                                                             Integer targetId,
                                                                             Pageable pageable);

    OrganisationUnit createOrganisationalUnit(
        OrganisationUnitDTORequest organisationUnitDTORequest);

    OrganisationUnit editOrganisationalUnit(OrganisationUnitDTORequest organisationUnitDTORequest,
                                            Integer organisationUnitId);

    OrganisationUnit editOrganisationalUnitApproveStatus(ApproveStatus approveStatus,
                                                         Integer organisationUnitId);

    void deleteOrganisationUnit(Integer organisationUnitId);

    OrganisationUnitsRelation createOrganisationUnitsRelation(
        OrganisationUnitsRelationDTO organisationUnitsRelation);

    void editOrganisationUnitsRelation(OrganisationUnitsRelationDTO organisationUnitsRelation,
                                       Integer id);

    void deleteOrganisationUnitsRelation(Integer id);

    void approveRelation(Integer relationId, Boolean approve);

    void addRelationProofs(List<DocumentFileDTO> documentFiles, Integer relationId);

    void deleteRelationProof(Integer relationId, Integer proofId);

    boolean recursiveCheckIfOrganisationUnitBelongsTo(Integer organisationUnitId,
                                                      Integer belongOrganisationUnit);

}
