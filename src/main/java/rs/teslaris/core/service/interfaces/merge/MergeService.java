package rs.teslaris.core.service.interfaces.merge;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;

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

    void saveMergedProceedingsMetadata(Integer leftId, Integer rightId, ProceedingsDTO leftData,
                                       ProceedingsDTO rightData);

    void saveMergedPersonsMetadata(Integer leftId, Integer rightId, PersonalInfoDTO leftData,
                                   PersonalInfoDTO rightData);

    void saveMergedJournalsMetadata(Integer leftId, Integer rightId, JournalDTO leftData,
                                    JournalDTO rightData);

    void saveMergedConferencesMetadata(Integer leftId, Integer rightId, ConferenceDTO leftData,
                                       ConferenceDTO rightData);

    void saveMergedSoftwareMetadata(Integer leftId, Integer rightId, SoftwareDTO leftData,
                                    SoftwareDTO rightData);

    void saveMergedDatasetsMetadata(Integer leftId, Integer rightId, DatasetDTO leftData,
                                    DatasetDTO rightData);

    void saveMergedPatentsMetadata(Integer leftId, Integer rightId, PatentDTO leftData,
                                   PatentDTO rightData);
}
