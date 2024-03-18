package rs.teslaris.core.importer.model.converter.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.importer.model.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.model.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@Component
@Slf4j
public class ProceedingsPublicationConverter extends DocumentConverter implements
    RecordConverter<Publication, ProceedingsPublicationDTO> {

    private final DocumentPublicationService documentPublicationService;


    @Autowired
    public ProceedingsPublicationConverter(
        MultilingualContentConverter multilingualContentConverter,
        PersonContributionConverter personContributionConverter,
        DocumentPublicationService documentPublicationService) {
        super(multilingualContentConverter, personContributionConverter);
        this.documentPublicationService = documentPublicationService;
    }

    @Override
    public ProceedingsPublicationDTO toDTO(Publication record) {
        var dto = new ProceedingsPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        if (record.getType().endsWith("c_5794")) {
            dto.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        } else {
            dto.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_ABSTRACT_ARTICLE);
        }

        setCommonFields(record, dto);

        dto.setArticleNumber(record.getNumber());
        dto.setStartPage(record.getStartPage());
        dto.setEndPage(record.getEndPage());

        var proceedings = documentPublicationService.findDocumentByOldId(
            OAIPMHParseUtility.parseBISISID(record.getPartOf().getPublication().getOldId()));
        if (Objects.isNull(proceedings)) {
            log.warn(
                "No saved proceedings with id: " + record.getPartOf().getPublication().getOldId());
            return null;
        }
        dto.setProceedingsId(proceedings.getId());
        dto.setEventId(proceedings.getEvent().getId());

        return dto;
    }
}
