package rs.teslaris.revisioner.restorer.publicationseries;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

/**
 * {@code JournalResponseDTO} (what the hydrator reads) and {@code JournalDTO} (what the update
 * method takes) are siblings rather than a hierarchy, so the revision is deserialized straight into
 * the update DTO. The only field lost is {@code languageTagNames}, which is display-only and
 * derived from {@code languageTagIds}.
 */
@Component
@RequiredArgsConstructor
public class JournalRevisionRestorer implements RevisionRestorer<JournalDTO> {

    private final JournalService journalService;


    @Override
    public String entityType() {
        return EntityType.JOURNAL.name();
    }

    @Override
    public Class<JournalDTO> dtoClass() {
        return JournalDTO.class;
    }

    @Override
    public void restore(Integer entityId, JournalDTO dto) {
        journalService.updateJournal(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return journalService.readJournal(entityId);
    }
}
