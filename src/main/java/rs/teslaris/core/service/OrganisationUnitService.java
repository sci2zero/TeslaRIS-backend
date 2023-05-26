package rs.teslaris.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;

@Service
public interface OrganisationUnitService {

    OrganisationUnit findOrganisationUnitById(Integer id);

    OrganisationUnitsRelation findOrganisationUnitsRelationById(Integer id);

    Page<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(Integer sourceId,
                                                                             Integer targetId,
                                                                             Pageable pageable);

    OrganisationUnit createOrganisationalUnit(OrganisationUnitDTO organisationUnitDTO);

    OrganisationUnitsRelation createOrganisationUnitsRelation(
        OrganisationUnitsRelationDTO organisationUnitsRelation);

    void editOrganisationUnitsRelation(OrganisationUnitsRelationDTO organisationUnitsRelation,
                                       Integer id);

    void deleteOrganisationUnitsRelation(Integer id);
}
