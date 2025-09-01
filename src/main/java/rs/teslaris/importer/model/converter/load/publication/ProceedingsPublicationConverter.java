package rs.teslaris.importer.model.converter.load.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.importer.dto.ProceedingsPublicationLoadDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.CommonRecordConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class ProceedingsPublicationConverter extends DocumentConverter implements
    RecordConverter<Publication, ProceedingsPublicationDTO>,
    CommonRecordConverter<DocumentImport, ProceedingsPublicationLoadDTO> {

    private final DocumentPublicationService documentPublicationService;


    @Autowired
    public ProceedingsPublicationConverter(
        MultilingualContentConverter multilingualContentConverter,
        PublisherConverter publisherConverter,
        BookSeriesService bookSeriesService,
        JournalService journalService,
        PersonContributionConverter personContributionConverter,
        DocumentPublicationService documentPublicationService) {
        super(multilingualContentConverter, publisherConverter, bookSeriesService, journalService,
            personContributionConverter);
        this.documentPublicationService = documentPublicationService;
    }

    @Override
    public ProceedingsPublicationDTO toDTO(Publication record) {
        var dto = new ProceedingsPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        var isInvited =
            Objects.nonNull(record.getInvited()) && record.getInvited().equals(Boolean.TRUE);
        if (record.getType().endsWith("c_5794")) {
            dto.setProceedingsPublicationType(
                isInvited ? ProceedingsPublicationType.INVITED_FULL_ARTICLE :
                    ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        } else {
            dto.setProceedingsPublicationType(
                isInvited ? ProceedingsPublicationType.INVITED_ABSTRACT_ARTICLE :
                    ProceedingsPublicationType.REGULAR_ABSTRACT_ARTICLE);
        }

        setCommonFields(record, dto);

        dto.setArticleNumber(record.getNumber());
        dto.setStartPage(record.getStartPage());
        dto.setNumberOfPages(record.getNumberOfPages());

        if (Objects.nonNull(dto.getStartPage()) && dto.getStartPage().contains("\\")) {
            dto.setStartPage(dto.getStartPage().replace("\\", ""));
        }

        dto.setEndPage(record.getEndPage());

        var proceedings = documentPublicationService.findDocumentByOldId(
            OAIPMHParseUtility.parseBISISID(record.getPartOf().getPublication().getOldId()));
        if (Objects.isNull(proceedings)) {
            log.warn("No saved proceedings with id: {}",
                record.getPartOf().getPublication().getOldId());
            return null;
        } else if (Objects.isNull(proceedings.getEvent())) {
            log.warn(
                "The following proceedings is not associated with an event: {}. Skipping loading publication...",
                record.getPartOf().getPublication().getOldId());
            return null;
        }

        dto.setProceedingsId(proceedings.getId());
        dto.setEventId(proceedings.getEvent().getId());

        return dto;
    }

    @Override
    public ProceedingsPublicationLoadDTO toImportDTO(DocumentImport document) {
        var dto = new ProceedingsPublicationLoadDTO();

        setCommonFields(document, dto);

        dto.setProceedingsName(multilingualContentConverter.toLoaderDTO(document.getPublishedIn()));
        dto.setConferenceName(
            multilingualContentConverter.toLoaderDTO(document.getEvent().getName()));
        dto.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        dto.setArticleNumber(document.getArticleNumber());
        dto.setNumberOfPages(document.getNumberOfPages());
        dto.setStartPage(document.getStartPage());
        dto.setEndPage(document.getEndPage());
        dto.setConfId(document.getEvent().getConfId());
        dto.setEIssn(document.getEIssn());
        dto.setPrintIssn(document.getPrintIssn());
        dto.setEventDateFrom(document.getEvent().getDateFrom().toString());
        dto.setEventDateTo(document.getEvent().getDateTo().toString());
        dto.setIsbn(document.getIsbn());

        return dto;
    }
}
