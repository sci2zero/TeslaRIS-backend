package rs.teslaris.revisioner.hydrator;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesLookupService;
import rs.teslaris.core.service.interfaces.document.PublisherService;

@Component
public class MonographRevisionHydrator extends RevisionHydrator<MonographDTO> {

    private final PublisherService publisherService;

    private final PublicationSeriesLookupService publicationSeriesLookupService;


    @Autowired
    public MonographRevisionHydrator(
        CountryService countryService, PublisherService publisherService,
        PublicationSeriesLookupService publicationSeriesLookupService) {
        super(countryService);
        this.publisherService = publisherService;
        this.publicationSeriesLookupService = publicationSeriesLookupService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.MONOGRAPH.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(MonographDTO dto) {
        hydrateCommonFields(dto);

        if (Objects.nonNull(dto.getPublisherId())) {
            dto.setPublisherName(MultilingualContentConverter.getMultilingualContentDTO(
                publisherService.findOne(dto.getPublisherId()).getName()));
        }

        if (Objects.nonNull(dto.getPublicationSeriesId())) {
            dto.setPublicationSeriesName(MultilingualContentConverter.getMultilingualContentDTO(
                publicationSeriesLookupService.fastPublicationSeriesLookup(
                    dto.getPublicationSeriesId()).getTitle()));
        }
    }
}
