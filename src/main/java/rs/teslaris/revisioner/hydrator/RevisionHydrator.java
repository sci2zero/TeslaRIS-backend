package rs.teslaris.revisioner.hydrator;

import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.search.CollectionOperations;

@RequiredArgsConstructor
public abstract class RevisionHydrator<T> {

    private final CountryService countryService;

    // Field injected, because most hydrators do not use documents.
    @Autowired
    private ResearchAreaService researchAreaService;


    public String entityType() {
        return "DOCUMENT";
    }

    public void hydrate(T dto) {
        // pass
    }

    protected void hydrateCommonFields(DocumentDTO dto) {
        if (Objects.nonNull(dto.getCountryId())) {
            dto.setCountryName(MultilingualContentConverter.getMultilingualContentDTO(
                countryService.findOne(dto.getCountryId()).getName()));
        }

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
