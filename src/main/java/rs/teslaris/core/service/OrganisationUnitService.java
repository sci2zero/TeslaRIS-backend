package rs.teslaris.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Service
public interface OrganisationUnitService {

    OrganisationUnit findOrganisationUnitById(Integer id);

    OrganisationUnit getReferenceToOrganisationUnitById(Integer id);

    Page<OrganisationUnit> findOrganisationUnits(Pageable pageable);

    OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id);

    Page<OrganisationUnitsRelation> getOrganisationUnitsRelations(Integer sourceId,
                                                                  Integer targetId,
                                                                  Pageable pageable);

    OrganisationUnit createOrganisationalUnit(OrganisationUnitDTORequest organisationUnitDTORequest);

    OrganisationUnit editOrganisationalUnit(OrganisationUnitDTORequest organisationUnitDTORequest, Integer organisationUnitId);

    void deleteOrganisationalUnit(Integer organisationUnitId);

    OrganisationUnitsRelation createOrganisationUnitsRelation(
        OrganisationUnitsRelationDTO organisationUnitsRelation);

    void editOrganisationUnitsRelation(OrganisationUnitsRelationDTO organisationUnitsRelation,
                                       Integer id);

    void deleteOrganisationUnitsRelation(Integer id);
}
