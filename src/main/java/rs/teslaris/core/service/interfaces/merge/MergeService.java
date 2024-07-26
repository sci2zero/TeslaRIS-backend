package rs.teslaris.core.service.interfaces.merge;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface MergeService {

    void switchJournalPublicationToOtherJournal(Integer targetJournalId, Integer publicationId);

    void switchAllPublicationsToOtherJournal(Integer sourceId, Integer targetId);

    void switchProceedingsPublicationToOtherProceedings(Integer targetProceedingsId,
                                                        Integer publicationId);

    void switchAllPublicationsToOtherProceedings(Integer sourceId, Integer targetId);

    void switchPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId,
                                        Integer publicationId);

    void switchAllPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId);

    void switchPersonToOtherOU(Integer sourceOUId, Integer targetOUId, Integer personId);

    void switchAllPersonsToOtherOU(Integer sourceOUId, Integer targetOUId);

    void switchProceedingsToOtherConference(Integer targetConferenceId, Integer proceedingsId);

    void switchAllProceedingsToOtherConference(Integer sourceConferenceId,
                                               Integer targetConferenceId);

    void switchInvolvements(List<Integer> involvementIds, Integer sourcePersonId,
                            Integer targetPersonId);

    void switchSkills(List<Integer> skillIds, Integer sourcePersonId, Integer targetPersonId);

    void switchPrizes(List<Integer> prizeIds, Integer sourcePersonId, Integer targetPersonId);
}
