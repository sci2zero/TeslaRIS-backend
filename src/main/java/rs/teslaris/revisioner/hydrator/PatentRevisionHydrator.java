package rs.teslaris.revisioner.hydrator;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.PublisherService;

@Component
public class PatentRevisionHydrator extends RevisionHydrator<PatentDTO> {

    private final PublisherService publisherService;


    @Autowired
    public PatentRevisionHydrator(
        CountryService countryService, PublisherService publisherService) {
        super(countryService);
        this.publisherService = publisherService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.PATENT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(PatentDTO dto) {
        hydrateCommonFields(dto);

        if (Objects.nonNull(dto.getPublisherId())) {
            dto.setPublisherName(MultilingualContentConverter.getMultilingualContentDTO(
                publisherService.findOne(dto.getPublisherId()).getName()));
        }
    }
}
