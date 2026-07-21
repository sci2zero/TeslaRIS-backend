package rs.teslaris.revisioner.hydrator.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class JournalPublicationRevisionHydrator
    extends RevisionHydrator<JournalPublicationResponseDTO> {

    private final JournalService journalService;


    @Autowired
    public JournalPublicationRevisionHydrator(
        CountryService countryService, JournalService journalService) {
        super(countryService);
        this.journalService = journalService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.JOURNAL_PUBLICATION.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(JournalPublicationResponseDTO dto) {
        hydrateCommonFields(dto);

        dto.setJournalName(MultilingualContentConverter.getMultilingualContentDTO(
            journalService.findJournalById(dto.getJournalId()).getTitle()));
    }
}
