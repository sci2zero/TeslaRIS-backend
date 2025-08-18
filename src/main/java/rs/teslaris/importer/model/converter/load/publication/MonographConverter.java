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
                              PersonContributionConverter personContributionConverter,
                              LanguageTagService languageTagService) {
        super(multilingualContentConverter, personContributionConverter);
        this.languageTagService = languageTagService;
    }

    @Override
    public MonographDTO toDTO(Publication record) {
        var dto = new MonographDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setMonographType(MonographType.BOOK);
        dto.setEisbn(record.getIsbn());

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

        return dto;
    }
}
