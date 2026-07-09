package rs.teslaris.revisioner.hydrator.document;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class DatasetRevisionHydrator extends RevisionHydrator<DatasetDTO> {

    private final PublisherService publisherService;


    @Autowired
    public DatasetRevisionHydrator(CountryService countryService,
                                   PublisherService publisherService) {
        super(countryService);
        this.publisherService = publisherService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.DATASET.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(DatasetDTO dto) {
        hydrateCommonFields(dto);

        if (Objects.nonNull(dto.getPublisherId())) {
            dto.setPublisherName(
                MultilingualContentConverter.getMultilingualContentDTO(
                    publisherService.findOne(dto.getPublisherId()).getName()));
        }
    }
}
