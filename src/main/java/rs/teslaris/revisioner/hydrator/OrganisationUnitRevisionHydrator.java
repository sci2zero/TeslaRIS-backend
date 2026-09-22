package rs.teslaris.revisioner.hydrator;

import java.util.HashSet;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.impl.commontypes.ResearchAreaServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.util.search.CollectionOperations;

@Component
public class OrganisationUnitRevisionHydrator extends RevisionHydrator<OrganisationUnitDTO> {

    private final ResearchAreaServiceImpl researchAreaService;


    @Autowired
    public OrganisationUnitRevisionHydrator(
        CountryService countryService, ResearchAreaServiceImpl researchAreaService) {
        super(countryService);
        this.researchAreaService = researchAreaService;
    }

    @Override
    public String entityType() {
        return EntityType.ORGANISATION_UNIT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(OrganisationUnitDTO dto) {
        if (CollectionOperations.containsValues(dto.getResearchAreasId())) {
            if (Objects.isNull(dto.getResearchAreas())) {
                dto.setResearchAreas(new HashSet<>());
            }

            dto.getResearchAreasId().forEach(researchAreaId ->
                dto.getResearchAreas()
                    .add(ResearchAreaConverter.toDTO(researchAreaService.findOne(researchAreaId)))
            );
        }
    }
}
