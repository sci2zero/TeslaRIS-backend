package rs.teslaris.core.service.interfaces.institution;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.dto.institution.RelationGraphDataDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface OrganisationUnitService extends JPAService<OrganisationUnit> {

    OrganisationUnit findOrganisationUnitById(Integer id);

    OrganisationUnitDTO readOrganisationUnitById(Integer id);

    OrganisationUnitIndex findOrganisationUnitByImportId(String importId);

    OrganisationUnit findOrganisationUnitByOldId(Integer oldId);

    OrganisationUnitDTO readOrganisationUnitForOldId(Integer oldId);

    OrganisationUnit getReferenceToOrganisationUnitById(Integer id);

    Long getOrganisationUnitsCount();

    Page<OrganisationUnitDTO> findOrganisationUnits(Pageable pageable);

    Page<OrganisationUnitIndex> searchOrganisationUnits(List<String> tokens, Pageable pageable,
                                                        SearchRequestType searchType,
                                                        Integer personId,
                                                        Integer topLevelInstitutionId,
                                                        Boolean onlyReturnOnesWhichCanHarvest,
                                                        Boolean onlyIndependent,
                                                        ThesisType allowedThesisType);

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

    OrganisationUnitsRelationDTO addSubOrganisationUnit(Integer sourceId, Integer targetId);

    void editOrganisationUnitsRelation(OrganisationUnitsRelationDTO organisationUnitsRelation,
                                       Integer id);

    void deleteOrganisationUnitsRelation(Integer id);

    void approveRelation(Integer relationId, Boolean approve);

    void addRelationProofs(List<DocumentFileDTO> documentFiles, Integer relationId);

    void deleteRelationProof(Integer relationId, Integer proofId);

    boolean recursiveCheckIfOrganisationUnitBelongsTo(Integer organisationUnitId,
                                                      Integer belongOrganisationUnit);

    boolean checkIfInstitutionalAdminsExist(Integer organisationUnitId);

    CompletableFuture<Void> reindexOrganisationUnits();

    List<Integer> getSuperOUsHierarchyRecursive(Integer sourceOUId);

    boolean isIdentifierInUse(String identifier, Integer organisationUnitId);

    List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields);

    OrganisationUnit findOrganisationUnitByAccountingId(String accountingId);

    String setOrganisationUnitLogo(Integer organisationUnitId, ProfilePhotoOrLogoDTO logoDTO)
        throws IOException;

    void removeOrganisationUnitLogo(Integer organisationUnitId);

    void indexOrganisationUnit(OrganisationUnit organisationUnit, Integer organisationUnitId);

    void indexOrganisationUnit(OrganisationUnit organisationUnit);

    OrganisationUnit findRaw(Integer organisationUnitId);

    OrganisationUnitsRelation getSuperOrganisationUnitRelation(Integer organisationUnitId);
}
