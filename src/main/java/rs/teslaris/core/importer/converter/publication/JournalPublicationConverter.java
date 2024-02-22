package rs.teslaris.core.importer.converter.publication;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.document.JournalService;

@Component
@Slf4j
public class JournalPublicationConverter extends DocumentConverter
    implements RecordConverter<Publication, JournalPublicationDTO> {

    private final JournalService journalService;


    @Autowired
    public JournalPublicationConverter(MultilingualContentConverter multilingualContentConverter,
                                       PersonContributionConverter personContributionConverter,
                                       JournalService journalService) {
        super(multilingualContentConverter, personContributionConverter);
        this.journalService = journalService;
    }

    @Override
    public JournalPublicationDTO toDTO(Publication record) {
        var dto = new JournalPublicationDTO();
        dto.setOldId(OAIPMHParseUtility.parseBISISID(record.getId()));

        setCommonFields(record, dto);

        dto.setArticleNumber(record.getNumber());
        dto.setVolume(record.getVolume());
        dto.setIssue(record.getIssue());
        dto.setStartPage(record.getStartPage());
        dto.setEndPage(record.getEndPage());

        var journal = journalService.findJournalByOldId(
            OAIPMHParseUtility.parseBISISID(record.getPublishedIn().getPublication().getId()));
        if (Objects.isNull(journal)) {
            log.warn(
                "No saved journal with id: " + record.getPublishedIn().getPublication().getId());
            return null;
        }
        dto.setJournalId(journal.getId());

        return dto;
    }
}
