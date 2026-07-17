package rs.teslaris.revisioner.hydrator.document;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class IntellectualPropertyRevisionHydrator
    extends RevisionHydrator<IntellectualPropertyDTO> {

    private final PublisherService publisherService;


    @Autowired
    public IntellectualPropertyRevisionHydrator(
        CountryService countryService, PublisherService publisherService) {
        super(countryService);
        this.publisherService = publisherService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.INTELLECTUAL_PROPERTY.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(IntellectualPropertyDTO dto) {
        hydrateCommonFields(dto);

        if (Objects.nonNull(dto.getPublisherId())) {
            dto.setPublisherName(MultilingualContentConverter.getMultilingualContentDTO(
                publisherService.findOne(dto.getPublisherId()).getName()));
        }
    }
}
