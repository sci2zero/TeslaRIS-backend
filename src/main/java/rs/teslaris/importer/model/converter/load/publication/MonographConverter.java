package rs.teslaris.importer.model.converter.load.publication;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class MonographConverter extends DocumentConverter
    implements RecordConverter<Publication, MonographDTO> {

    private final LanguageTagService languageTagService;


    @Autowired
    public MonographConverter(MultilingualContentConverter multilingualContentConverter,
                              PublisherConverter publisherConverter,
                              BookSeriesService bookSeriesService,
                              PersonContributionConverter personContributionConverter,
                              LanguageTagService languageTagService) {
        super(multilingualContentConverter, publisherConverter, bookSeriesService,
            personContributionConverter);
        this.languageTagService = languageTagService;
    }

    @Override
    public MonographDTO toDTO(Publication record) {
        var dto = new MonographDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setMonographType(MonographType.BOOK);
        dto.setEisbn(record.getIsbn());
        dto.setVolume(record.getVolume());
        dto.setNumberOfPages(record.getNumberOfPages());

        // TODO: what to do with research area, we get them here as string but in our model they are separate entities

        if (Objects.nonNull(record.getLanguage())) {
            var languageTagValue = deduceLanguageTagValue(record);

            var languageTag = languageTagService.findLanguageTagByValue(languageTagValue);
            if (Objects.nonNull(languageTag.getId())) {
                dto.setLanguageTagIds(List.of(languageTag.getId()));
            } else {
                log.warn("No saved language with tag: {}", languageTagValue);
                return null;
            }
        } else {
            dto.setLanguageTagIds(Collections.emptyList());
        }

        if (Objects.nonNull(record.getPublisher())) {
            publisherConverter.setPublisherInformation(record.getPublisher(), dto);
        }

        if (Objects.nonNull(record.getBookSeries())) {
            setBookSeriesInformation(record.getBookSeries(), dto);
        }

        return dto;
    }
}
