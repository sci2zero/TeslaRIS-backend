package rs.teslaris.core.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.GeoLocationConverter;
import rs.teslaris.core.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.converter.institution.RelationConverter;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.SelfRelationException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.JPASoftDeleteRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.ResearchAreaService;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitServiceImpl extends JPAServiceImpl<OrganisationUnit>
    implements OrganisationUnitService {

    private final OrganisationUnitRepository organisationUnitRepository;

    private final MultilingualContentService multilingualContentService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final ResearchAreaService researchAreaService;
    private final DocumentFileService documentFileService;

    @Value("${relation.approved_by_default}")
    private Boolean relationApprovedByDefault;

    @Value("${organisation_unit.approved_by_default}")
    private Boolean organisationUnitApprovedByDefault;

    @Override
    protected JPASoftDeleteRepository<OrganisationUnit> getEntityRepository() {
        return organisationUnitRepository;
    }

    @Override
    public OrganisationUnit findOrganisationUnitById(Integer id) {
        return findOne(id);
    }

    @Override
    public OrganisationUnit findOne(Integer id) {
        return organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(id)
            .orElseThrow(
                () -> new NotFoundException("Organisation unit with given ID does not exist."));
    }

    @Override
    @Transactional
    public Page<OrganisationUnitDTO> findOrganisationUnits(Pageable pageable) {
        return organisationUnitRepository.findAllWithLangDataAndDeletedIsFalse(pageable)
            .map(OrganisationUnitConverter::toDTO);
    }

    @Override
    public OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id) {
        return organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(id).orElseThrow(
            () -> new NotFoundException(
                "Organisation units relation with given ID does not exist."));
    }

    @Override
    public Page<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(
        Integer sourceId, Integer targetId, Pageable pageable) {
        return organisationUnitsRelationRepository.getRelationsForOrganisationUnits(pageable,
            sourceId, targetId).map(RelationConverter::toResponseDTO);
    }

    @Override
    public OrganisationUnit getReferenceToOrganisationUnitById(Integer id) {
        return id == null ? null : organisationUnitRepository.getReferenceById(id);
    }

    @Override
    public OrganisationUnit createOrganisationalUnit(
        OrganisationUnitDTORequest organisationUnitDTORequest) {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            multilingualContentService.getMultilingualContent(organisationUnitDTORequest.getName())
        );
        organisationUnit.setNameAbbreviation(organisationUnitDTORequest.getNameAbbreviation());
        organisationUnit.setKeyword(
            multilingualContentService.getMultilingualContent(
                organisationUnitDTORequest.getKeyword())
        );

        List<ResearchArea> researchAreas = researchAreaService.getResearchAreasByIds(
            organisationUnitDTORequest.getResearchAreasId());
        organisationUnit.setResearchAreas(new HashSet<>(researchAreas));

        organisationUnit.setLocation(
            GeoLocationConverter.fromDTO(organisationUnitDTORequest.getLocation()));

        if (organisationUnitApprovedByDefault) {
            organisationUnit.setApproveStatus(ApproveStatus.APPROVED);
        } else {
            organisationUnit.setApproveStatus(ApproveStatus.REQUESTED);
        }

        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTORequest.getContact()));

        organisationUnit = organisationUnitRepository.save(organisationUnit);


        return organisationUnit;
    }

    @Override
    public OrganisationUnit editOrganisationalUnit(
        OrganisationUnitDTORequest organisationUnitDTORequest, Integer organisationUnitId) {

        OrganisationUnit organisationUnit = getReferenceToOrganisationUnitById(organisationUnitId);

        organisationUnit.getName().clear();
        organisationUnit.setName(
            multilingualContentService.getMultilingualContent(organisationUnitDTORequest.getName())
        );

        organisationUnit.setNameAbbreviation(organisationUnitDTORequest.getNameAbbreviation());

        organisationUnit.getKeyword().clear();
        organisationUnit.setKeyword(
            multilingualContentService.getMultilingualContent(
                organisationUnitDTORequest.getKeyword())
        );

        organisationUnit.getResearchAreas().clear();
        List<ResearchArea> researchAreas = researchAreaService.getResearchAreasByIds(
            organisationUnitDTORequest.getResearchAreasId());
        organisationUnit.setResearchAreas(new HashSet<>(researchAreas));

        organisationUnit.setLocation(
            GeoLocationConverter.fromDTO(organisationUnitDTORequest.getLocation()));

        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTORequest.getContact()));

        organisationUnit = organisationUnitRepository.save(organisationUnit);

        return organisationUnit;
    }

    @Override
    @Transactional
    public OrganisationUnit editOrganisationalUnitApproveStatus(ApproveStatus approveStatus,
                                                                Integer organisationUnitId) {
        OrganisationUnit organisationUnit =
            organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(
                    organisationUnitId)
                .orElseThrow(
                    () -> new NotFoundException(
                        "Organisation units relation with given ID does not exist."));

        organisationUnit.setApproveStatus(approveStatus);

        organisationUnitRepository.save(organisationUnit);
        return organisationUnit;
    }

    @Override
    @Deprecated(forRemoval = true)
    public void deleteOrganisationalUnit(Integer organisationUnitId) {
        var organisationUnitReference = getReferenceToOrganisationUnitById(organisationUnitId);
        organisationUnitRepository.delete(organisationUnitReference);
    }

    @Override
    public OrganisationUnitsRelation createOrganisationUnitsRelation(
        OrganisationUnitsRelationDTO relationDTO) {
        if (Objects.equals(relationDTO.getSourceOrganisationUnitId(),
            relationDTO.getTargetOrganisationUnitId())) {
            throw new SelfRelationException("Organisation unit cannot relate to itself.");
        }

        var newRelation = new OrganisationUnitsRelation();
        setCommonFields(newRelation, relationDTO);

        if (relationApprovedByDefault) {
            newRelation.setApproveStatus(ApproveStatus.APPROVED);
        } else {
            newRelation.setApproveStatus(ApproveStatus.REQUESTED);
        }

        return organisationUnitsRelationRepository.save(newRelation);
    }

    @Override
    public void editOrganisationUnitsRelation(OrganisationUnitsRelationDTO relationDTO,
                                              Integer id) {
        var relationToUpdate = findOrganisationUnitsRelationById(id);

        relationToUpdate.getSourceAffiliationStatement().clear();
        relationToUpdate.getTargetAffiliationStatement().clear();

        setCommonFields(relationToUpdate, relationDTO);

        organisationUnitsRelationRepository.save(relationToUpdate);
    }

    @Override
    public void deleteOrganisationUnitsRelation(Integer id) {
        var relationToDelete = organisationUnitsRelationRepository.getReferenceById(id);
        organisationUnitsRelationRepository.delete(relationToDelete);
    }

    @Override
    public void approveRelation(Integer relationId, Boolean approve) {
        var relationToApprove = findOrganisationUnitsRelationById(relationId);
        relationToApprove.setApproveStatus(
            approve ? ApproveStatus.APPROVED : ApproveStatus.DECLINED);
        organisationUnitsRelationRepository.save(relationToApprove);
    }

    private void setCommonFields(OrganisationUnitsRelation relation,
                                 OrganisationUnitsRelationDTO relationDTO) {
        relation.setSourceAffiliationStatement(multilingualContentService.getMultilingualContent(
            relationDTO.getSourceAffiliationStatement()));
        relation.setTargetAffiliationStatement(multilingualContentService.getMultilingualContent(
            relationDTO.getTargetAffiliationStatement()));

        relation.setRelationType(relationDTO.getRelationType());
        relation.setDateFrom(relationDTO.getDateFrom());
        relation.setDateTo(relationDTO.getDateTo());

        relation.setSourceOrganisationUnit(
            findOrganisationUnitById(relationDTO.getSourceOrganisationUnitId()));
        relation.setTargetOrganisationUnit(
            findOrganisationUnitById(relationDTO.getTargetOrganisationUnitId()));
    }

    @Override
    public void addRelationProofs(List<DocumentFileDTO> proofs, Integer relationId) {
        var relation = findOrganisationUnitsRelationById(relationId);
        proofs.forEach(proof -> {
            var documentFile = documentFileService.saveNewDocument(proof, true);
            relation.getProofs().add(documentFile);
            organisationUnitsRelationRepository.save(relation);
        });
    }

    @Override
    public void deleteRelationProof(Integer relationId, Integer proofId) {
        var documentFile = documentFileService.findDocumentFileById(proofId);
        documentFileService.delete(proofId);
        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    @Override
    public boolean recursiveCheckIfOrganisationUnitBelongsTo(Integer sourceOrganisationUnitId,
                                                             Integer targetOrganisationUnit) {
        List<OrganisationUnitsRelation> relationsToCheck =
            organisationUnitsRelationRepository.findBySourceOrganisationUnitAndRelationTypeAndDeletedIsFalse(
                sourceOrganisationUnitId, OrganisationUnitRelationType.BELONGS_TO);

        while (!relationsToCheck.isEmpty()) {
            OrganisationUnitsRelation newTargetOrganisationUnit = relationsToCheck.remove(0);

            if (newTargetOrganisationUnit.getId().equals(targetOrganisationUnit)) {
                return true;
            }

            List<OrganisationUnitsRelation> newRelationToCheck =
                organisationUnitsRelationRepository.findBySourceOrganisationUnitAndRelationTypeAndDeletedIsFalse(
                    newTargetOrganisationUnit.getId(), OrganisationUnitRelationType.BELONGS_TO);
            relationsToCheck.addAll(newRelationToCheck);

        }
        return false;

    }
}
