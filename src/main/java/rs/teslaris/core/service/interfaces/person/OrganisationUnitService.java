package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.dto.institution.RelationGraphDataDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface OrganisationUnitService extends JPAService<OrganisationUnit> {

    OrganisationUnit findOrganisationUnitById(Integer id);

    OrganisationUnitIndex findOrganisationUnitByScopusAfid(String scopusAfid);

    OrganisationUnit findOrganisationUnitByOldId(Integer oldId);

    OrganisationUnitDTO readOrganisationUnitForOldId(Integer oldId);

    OrganisationUnit getReferenceToOrganisationUnitById(Integer id);

    Long getOrganisationUnitsCount();

    Page<OrganisationUnitDTO> findOrganisationUnits(Pageable pageable);

    Page<OrganisationUnitIndex> searchOrganisationUnits(List<String> tokens, Pageable pageable,
                                                        SearchRequestType searchType);

    OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id);

    List<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(Integer sourceId);

    RelationGraphDataDTO getOrganisationUnitsRelationsChain(Integer leafId);

    List<Integer> getOrganisationUnitIdsFromSubHierarchy(Integer currentOUNodeId);

    Page<OrganisationUnitIndex> getOUSubUnits(Integer organisationUnitId, Pageable pageable);

    OrganisationUnitDTO createOrganisationUnit(
        OrganisationUnitRequestDTO organisationUnitRequestDTO, Boolean index);

    OrganisationUnit editOrganisationUnit(Integer organisationUnitId,
                                          OrganisationUnitRequestDTO organisationUnitDTORequest);

    OrganisationUnit editOrganisationalUnitApproveStatus(ApproveStatus approveStatus,
                                                         Integer organisationUnitId);

    void deleteOrganisationUnit(Integer organisationUnitId);

    void forceDeleteOrganisationUnit(Integer organisationUnitId);

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

    boolean checkIfInstitutionalAdminsExist(Integer organisationUnitId);

    void reindexOrganisationUnits();
}
