package rs.teslaris.revisioner.restorer.institution;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class OrganisationUnitRevisionRestorer implements RevisionRestorer<OrganisationUnitDTO> {

    private final OrganisationUnitService organisationUnitService;


    @Override
    public String entityType() {
        return EntityType.ORGANISATION_UNIT.name();
    }

    @Override
    public Class<OrganisationUnitDTO> dtoClass() {
        return OrganisationUnitDTO.class;
    }

    @Override
    public void restore(Integer entityId, OrganisationUnitDTO dto) {
        organisationUnitService.editOrganisationUnit(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return organisationUnitService.readOrganisationUnitById(entityId);
    }
}
