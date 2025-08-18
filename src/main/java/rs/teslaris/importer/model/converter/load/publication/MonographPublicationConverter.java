package rs.teslaris.importer.model.converter.load.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.model.document.MonographPublicationType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class MonographPublicationConverter extends DocumentConverter implements
    RecordConverter<Publication, MonographPublicationDTO> {

    private final DocumentPublicationService documentPublicationService;


    @Autowired
    public MonographPublicationConverter(
        MultilingualContentConverter multilingualContentConverter,
        PersonContributionConverter personContributionConverter,
        DocumentPublicationService documentPublicationService) {
        super(multilingualContentConverter, personContributionConverter);
        this.documentPublicationService = documentPublicationService;
    }

    @Override
    public MonographPublicationDTO toDTO(Publication record) {
        var dto = new MonographPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        if (record.getType().endsWith("c_3248")) {
            dto.setMonographPublicationType(MonographPublicationType.CHAPTER);
        } else {
            dto.setMonographPublicationType(MonographPublicationType.RESEARCH_ARTICLE);
        }

        setCommonFields(record, dto);

        dto.setArticleNumber(record.getNumber());
        dto.setStartPage(record.getStartPage());

        if (Objects.nonNull(dto.getStartPage()) && dto.getStartPage().contains("\\")) {
            dto.setStartPage(dto.getStartPage().replace("\\", ""));
        }

        dto.setEndPage(record.getEndPage());

        var monograph = documentPublicationService.findDocumentByOldId(
            OAIPMHParseUtility.parseBISISID(record.getPartOf().getPublication().getOldId()));
        if (Objects.isNull(monograph)) {
            log.warn("No saved monograph with id: {}",
                record.getPartOf().getPublication().getOldId());
            return null;
        }
        dto.setMonographId(monograph.getId());

        return dto;
    }
}
