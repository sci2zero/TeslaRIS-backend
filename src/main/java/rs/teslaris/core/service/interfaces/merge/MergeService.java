package rs.teslaris.core.service.interfaces.merge;

import org.springframework.stereotype.Service;

@Service
public interface MergeService {

    void switchJournalPublicationToOtherJournal(Integer targetJournalId, Integer publicationId);

    void switchAllPublicationsToOtherJournal(Integer sourceId, Integer targetId);
}
