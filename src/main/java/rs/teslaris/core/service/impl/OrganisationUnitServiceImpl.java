package rs.teslaris.core.service.impl;

import java.util.HashSet;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.GeoLocationDTOToGeoLocation;
import rs.teslaris.core.converter.person.ContactConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.person.OrganisationalUnitRepository;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.OrganisationUnitService;
import rs.teslaris.core.service.ResearchAreaService;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganisationUnitServiceImpl implements OrganisationUnitService {

    private final OrganisationalUnitRepository organisationalUnitRepository;

    private final MultilingualContentService multilingualContentService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final ResearchAreaService researchAreaService;

    @Value("${relation.approved_by_default}")
    private Boolean approvedByDefault;


    @Override
    public OrganisationUnit findOrganisationUnitById(Integer id) {
        return organisationalUnitRepository.findById(id).orElseThrow(
            () -> new NotFoundException("Organisation unit with given ID does not exist."));
    }

    @Override
    public Page<OrganisationUnit> findOrganisationUnits(Pageable pageable) {
        return organisationalUnitRepository.findAll(pageable);
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
    public OrganisationUnit getReferenceToOrganisationUnitById(Integer id) {
        return id == null ? null : organisationalUnitRepository.getReferenceById(id);
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
            GeoLocationDTOToGeoLocation.fromDTO(organisationUnitDTORequest.getLocation()));

        organisationUnit.setApproveStatus(organisationUnitDTORequest.getApproveStatus());
        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTORequest.getContact()));

        organisationUnit = organisationalUnitRepository.save(organisationUnit);


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
            GeoLocationDTOToGeoLocation.fromDTO(organisationUnitDTORequest.getLocation()));

        organisationUnit.setApproveStatus(organisationUnitDTORequest.getApproveStatus());
        organisationUnit.setContact(
            ContactConverter.fromDTO(organisationUnitDTORequest.getContact()));

        organisationUnit = organisationalUnitRepository.save(organisationUnit);

        return organisationUnit;
    }

    @Override
    public void deleteOrganisationalUnit(Integer organisationUnitId) {
        var organisationUnitReference = getReferenceToOrganisationUnitById(organisationUnitId);
        organisationalUnitRepository.delete(organisationUnitReference);
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
