package rs.teslaris.revisioner.hydrator;

import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.impl.commontypes.ResearchAreaServiceImpl;
import rs.teslaris.core.util.search.CollectionOperations;

@Component
@RequiredArgsConstructor
public class IntangibleProductRevisionHydrator
    implements RevisionHydrator<IntangibleProductDTO> {

    private final ResearchAreaServiceImpl researchAreaService;

    @Override
    public String entityType() {
        return DocumentPublicationType.INTANGIBLE_PRODUCT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(IntangibleProductDTO dto) {
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
