package rs.teslaris.core.service.impl;

import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.person.OrganisationalUnitRepository;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.OrganisationUnitService;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitServiceImpl implements OrganisationUnitService {

    private final OrganisationalUnitRepository organisationalUnitRepository;

    private final MultilingualContentService multilingualContentService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    @Value("${relation.approved_by_default}")
    private Boolean approvedByDefault;


    @Override
    public OrganisationUnit findOrganisationUnitById(Integer id) {
        return organisationalUnitRepository.findById(id).orElseThrow(
            () -> new NotFoundException("Organisation unit with given ID does not exist."));
    }

    @Override
    public OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id) {
        return organisationUnitsRelationRepository.findById(id).orElseThrow(
            () -> new NotFoundException(
                "Organisation units relation with given ID does not exist."));
    }

    @Override
    public Page<OrganisationUnitsRelation> getOrganisationUnitsRelations(Integer sourceId,
                                                                         Integer targetId,
                                                                         Pageable pageable) {
        return organisationUnitsRelationRepository.getRelationsForOrganisationUnits(pageable,
            sourceId, targetId);
    }

    @Override
    public OrganisationUnit createOrganisationalUnit(OrganisationUnitDTO organisationUnitDTO) {


//        OrganisationUnit newOrganisationUnit = new OrganisationUnit();
//        newOrganisationUnit.setName(new HashSet<>(Set.of(new MultiLingualContent(organisationUnitDTO.))));

        return null;
    }

    @Override
    public OrganisationUnitsRelation createOrganisationUnitsRelation(
        OrganisationUnitsRelationDTO relationDTO) {
        var newRelation = new OrganisationUnitsRelation();
        setCommonFields(newRelation, relationDTO);

        if (approvedByDefault) {
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

    private void setCommonFields(OrganisationUnitsRelation relation,
                                 OrganisationUnitsRelationDTO relationDTO) {
        relation.setSourceAffiliationStatement(multilingualContentService.getMultilingualContent(
            relationDTO.getSourceAffiliationStatement()));
        relation.setTargetAffiliationStatement(multilingualContentService.getMultilingualContent(
            relationDTO.getTargetAffiliationStatement()));

        relation.setRelationType(relationDTO.getRelationType());
        relation.setDateFrom(relationDTO.getDateFrom());
        relation.setDateTo(relationDTO.getDateTo());

        relation.setSourceOrganisationUnit(organisationalUnitRepository.getReferenceById(
            relationDTO.getSourceOrganisationUnitId()));
        relation.setTargetOrganisationUnit(organisationalUnitRepository.getReferenceById(
            relationDTO.getTargetOrganisationUnitId()));
    }
}
