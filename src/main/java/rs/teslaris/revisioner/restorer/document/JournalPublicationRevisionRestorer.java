package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class JournalPublicationRevisionRestorer
    implements RevisionRestorer<JournalPublicationResponseDTO> {

    private final JournalPublicationService journalPublicationService;


    @Override
    public String entityType() {
        return DocumentPublicationType.JOURNAL_PUBLICATION.name();
    }

    @Override
    public Class<JournalPublicationResponseDTO> dtoClass() {
        return JournalPublicationResponseDTO.class;
    }

    @Override
    public void restore(Integer entityId, JournalPublicationResponseDTO dto) {
        journalPublicationService.editJournalPublication(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return journalPublicationService.readJournalPublicationById(entityId);
    }
}
