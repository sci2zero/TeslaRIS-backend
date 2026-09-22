package rs.teslaris.revisioner.hydrator.document;

import java.util.ArrayList;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.impl.commontypes.ResearchAreaServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class IntangibleProductRevisionHydrator extends RevisionHydrator<IntangibleProductDTO> {

    private final ResearchAreaServiceImpl researchAreaService;


    @Autowired
    public IntangibleProductRevisionHydrator(
        CountryService countryService, ResearchAreaServiceImpl researchAreaService) {
        super(countryService);
        this.researchAreaService = researchAreaService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.INTANGIBLE_PRODUCT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(IntangibleProductDTO dto) {
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
