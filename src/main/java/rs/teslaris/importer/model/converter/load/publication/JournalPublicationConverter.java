package rs.teslaris.importer.model.converter.load.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.oaipmh.publication.Publication;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.importer.dto.JournalPublicationLoadDTO;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.utility.CommonRecordConverter;
import rs.teslaris.importer.utility.RecordConverter;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Component
@Slf4j
public class JournalPublicationConverter extends DocumentConverter
    implements RecordConverter<Publication, JournalPublicationDTO>,
    CommonRecordConverter<DocumentImport, JournalPublicationLoadDTO> {

    private final JournalService journalService;


    @Autowired
    public JournalPublicationConverter(MultilingualContentConverter multilingualContentConverter,
                                       PublisherConverter publisherConverter,
                                       BookSeriesService bookSeriesService,
                                       PersonContributionConverter personContributionConverter,
                                       JournalService journalService) {
        super(multilingualContentConverter, publisherConverter, bookSeriesService,
            personContributionConverter);
        this.journalService = journalService;
    }

    @Override
    public JournalPublicationDTO toDTO(Publication record) {
        var dto = new JournalPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getOldId()));

        setCommonFields(record, dto);

        dto.setArticleNumber(record.getNumber());
        dto.setVolume(record.getVolume());
        dto.setIssue(record.getIssue());
        dto.setStartPage(record.getStartPage());
        dto.setEndPage(record.getEndPage());

        var journal = journalService.findJournalByOldId(
            OAIPMHParseUtility.parseBISISID(record.getPublishedIn().getPublication().getOldId()));
        if (Objects.isNull(journal)) {
            log.warn(
                "No saved journal with id: " + record.getPublishedIn().getPublication().getOldId());
            return null;
        }
        dto.setJournalId(journal.getId());

        return dto;
    }

    @Override
    public JournalPublicationLoadDTO toImportDTO(DocumentImport document) {
        var dto = new JournalPublicationLoadDTO();

        setCommonFields(document, dto);

        dto.setJournalName(multilingualContentConverter.toLoaderDTO(document.getPublishedIn()));
        dto.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        dto.setArticleNumber(document.getArticleNumber());
        dto.setVolume(document.getVolume());
        dto.setIssue(document.getIssue());
        dto.setStartPage(document.getStartPage());
        dto.setEndPage(document.getEndPage());
        dto.setJournalEIssn(document.getEIssn());
        dto.setJournalPrintIssn(document.getPrintIssn());
        dto.setJournalOpenAlexId(document.getJournalOpenAlexId());

        return dto;
    }
}
