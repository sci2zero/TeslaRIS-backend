package rs.teslaris.revisioner.hydrator;

import java.util.ArrayList;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.impl.commontypes.ResearchAreaServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.util.search.CollectionOperations;

@Component
public class MaterialProductRevisionHydrator extends RevisionHydrator<MaterialProductDTO> {

    private final ResearchAreaServiceImpl researchAreaService;


    @Autowired
    public MaterialProductRevisionHydrator(
        CountryService countryService, ResearchAreaServiceImpl researchAreaService) {
        super(countryService);
        this.researchAreaService = researchAreaService;
    }


    @Override
    public String entityType() {
        return DocumentPublicationType.MATERIAL_PRODUCT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(MaterialProductDTO dto) {
        hydrateCommonFields(dto);

        if (CollectionOperations.containsValues(dto.getResearchAreasId())) {
            if (Objects.isNull(dto.getResearchAreas())) {
                dto.setResearchAreas(new ArrayList<>());
            }

            dto.getResearchAreasId().forEach(researchAreaId ->
                dto.getResearchAreas()
                    .add(ResearchAreaConverter.toDTO(researchAreaService.findOne(researchAreaId)))
            );
        }
    }
}
