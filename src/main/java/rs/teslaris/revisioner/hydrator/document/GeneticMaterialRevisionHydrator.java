package rs.teslaris.revisioner.hydrator.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class GeneticMaterialRevisionHydrator extends RevisionHydrator<GeneticMaterialDTO> {

    @Autowired
    public GeneticMaterialRevisionHydrator(
        CountryService countryService) {
        super(countryService);
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.GENETIC_MATERIAL.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(GeneticMaterialDTO dto) {
        hydrateCommonFields(dto);
    }
}
