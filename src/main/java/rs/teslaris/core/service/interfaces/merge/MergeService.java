package rs.teslaris.core.service.interfaces.merge;

import org.springframework.stereotype.Service;

@Service
public interface MergeService {

    void switchJournalPublicationToOtherJournal(Integer targetJournalId, Integer publicationId);

    void switchAllPublicationsToOtherJournal(Integer sourceId, Integer targetId);

    void switchPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId,
                                        Integer publicationId);

    void switchAllPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId);

    void switchPersonToOtherOU(Integer sourceOUId, Integer targetOUId, Integer personId);

    void switchAllPersonsToOtherOU(Integer sourceOUId, Integer targetOUId);

    void switchProceedingsToOtherConference(Integer targetConferenceId, Integer proceedingsId);

    void switchAllProceedingsToOtherConference(Integer sourceConferenceId, Integer targetConferenceId);
}
