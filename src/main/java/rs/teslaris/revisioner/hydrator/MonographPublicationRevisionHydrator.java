package rs.teslaris.revisioner.hydrator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.MonographService;

@Component
public class MonographPublicationRevisionHydrator
    extends RevisionHydrator<MonographPublicationDTO> {

    private final MonographService monographService;


    @Autowired
    public MonographPublicationRevisionHydrator(
        CountryService countryService, MonographService monographService) {
        super(countryService);
        this.monographService = monographService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.MONOGRAPH_PUBLICATION.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(MonographPublicationDTO dto) {
        hydrateCommonFields(dto);

        dto.setMonographName(MultilingualContentConverter.getMultilingualContentDTO(
            monographService.findMonographById(dto.getMonographId()).getTitle()));
    }
}
