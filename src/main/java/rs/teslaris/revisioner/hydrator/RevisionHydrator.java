package rs.teslaris.revisioner.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;

@RequiredArgsConstructor
public abstract class RevisionHydrator<T> {

    private final CountryService countryService;


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
    }
}
