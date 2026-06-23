package rs.teslaris.revisioner.hydrator.document;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class ProceedingsRevisionHydrator extends RevisionHydrator<ProceedingsResponseDTO> {

    private final EventService eventService;

    private final PublisherService publisherService;


    @Autowired
    public ProceedingsRevisionHydrator(
        CountryService countryService, EventService eventService,
        PublisherService publisherService) {
        super(countryService);
        this.eventService = eventService;
        this.publisherService = publisherService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.PROCEEDINGS.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(ProceedingsResponseDTO dto) {
        hydrateCommonFields(dto);

        if (Objects.nonNull(dto.getEventId())) {
            dto.setEventName(MultilingualContentConverter.getMultilingualContentDTO(
                eventService.findOne(dto.getEventId()).getName()));
        }

        if (Objects.nonNull(dto.getPublisherId())) {
            dto.setPublisherName(MultilingualContentConverter.getMultilingualContentDTO(
                publisherService.findOne(dto.getPublisherId()).getName()));
        }
    }
}
