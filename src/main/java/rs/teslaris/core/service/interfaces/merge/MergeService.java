package rs.teslaris.core.service.interfaces.merge;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;

@Service
public interface MergeService {

    void switchJournalPublicationToOtherJournal(Integer targetJournalId, Integer publicationId);

    void switchAllPublicationsToOtherJournal(Integer sourceId, Integer targetId);

    void switchAllIndicatorsToOtherJournal(Integer sourceId, Integer targetId);

    void switchAllClassificationsToOtherJournal(Integer sourceId, Integer targetId);

    void switchPublisherPublicationToOtherPublisher(Integer targetPublisherId,
                                                    Integer publicationId);

    void switchAllPublicationsToOtherPublisher(Integer sourceId, Integer targetId);

    void switchPublicationToOtherBookSeries(Integer targetJournalId,
                                            Integer publicationId);

    void switchAllPublicationsToOtherBookSeries(Integer sourceId, Integer targetId);

    void switchProceedingsPublicationToOtherProceedings(Integer targetProceedingsId,
                                                        Integer publicationId);

    void switchAllPublicationsToOtherProceedings(Integer sourceId, Integer targetId);

    void switchPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId,
                                        Integer publicationId);

    void switchAllPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId);

    void switchPersonToOtherOU(Integer sourceOUId, Integer targetOUId, Integer personId);

    void switchAllPersonsToOtherOU(Integer sourceOUId, Integer targetOUId);

    void saveMergedOUsMetadata(Integer leftId, Integer rightId, OrganisationUnitRequestDTO leftData,
                               OrganisationUnitRequestDTO rightData);

    void switchProceedingsToOtherConference(Integer targetConferenceId, Integer proceedingsId);

    void switchAllProceedingsToOtherConference(Integer sourceConferenceId,
                                               Integer targetConferenceId);

    void switchAllIndicatorsToOtherEvent(Integer sourceId, Integer targetId);

    void switchAllClassificationsToOtherEvent(Integer sourceId, Integer targetId);

    void switchInvolvements(List<Integer> involvementIds, Integer sourcePersonId,
                            Integer targetPersonId);

    void switchSkills(List<Integer> skillIds, Integer sourcePersonId, Integer targetPersonId);

    void switchPrizes(List<Integer> prizeIds, Integer sourcePersonId, Integer targetPersonId);

    void saveMergedDocumentFiles(Integer leftId, Integer rightId,
                                 List<Integer> leftProofs,
                                 List<Integer> rightProofs,
                                 List<Integer> leftFileItems,
                                 List<Integer> rightFileItems);

    void saveMergedProceedingsMetadata(Integer leftId, Integer rightId, ProceedingsDTO leftData,
                                       ProceedingsDTO rightData);

    void saveMergedPersonsMetadata(Integer leftId, Integer rightId, PersonalInfoDTO leftData,
                                   PersonalInfoDTO rightData);

    void saveMergedJournalsMetadata(Integer leftId, Integer rightId, JournalDTO leftData,
                                    JournalDTO rightData);

    void saveMergedBookSeriesMetadata(Integer leftId, Integer rightId, BookSeriesDTO leftData,
                                      BookSeriesDTO rightData);

    void saveMergedConferencesMetadata(Integer leftId, Integer rightId, ConferenceDTO leftData,
                                       ConferenceDTO rightData);

    void saveMergedSoftwareMetadata(Integer leftId, Integer rightId, SoftwareDTO leftData,
                                    SoftwareDTO rightData);

    void saveMergedDatasetsMetadata(Integer leftId, Integer rightId, DatasetDTO leftData,
                                    DatasetDTO rightData);

    void saveMergedPatentsMetadata(Integer leftId, Integer rightId, PatentDTO leftData,
                                   PatentDTO rightData);

    void saveMergedProceedingsPublicationMetadata(Integer leftId, Integer rightId,
                                                  ProceedingsPublicationDTO leftData,
                                                  ProceedingsPublicationDTO rightData);

    void saveMergedJournalPublicationMetadata(Integer leftId, Integer rightId,
                                              JournalPublicationDTO leftData,
                                              JournalPublicationDTO rightData);

    void saveMergedThesesMetadata(Integer leftId, Integer rightId,
                                  ThesisDTO leftData,
                                  ThesisDTO rightData);

    void switchPublicationToOtherMonograph(Integer targetMonographId, Integer publicationId);

    void switchAllPublicationsToOtherMonograph(Integer sourceMonographId,
                                               Integer targetMonographId);

    void saveMergedMonographsMetadata(Integer leftId, Integer rightId,
                                      MonographDTO leftData,
                                      MonographDTO rightData);

    void saveMergedMonographPublicationsMetadata(Integer leftId, Integer rightId,
                                                 MonographPublicationDTO leftData,
                                                 MonographPublicationDTO rightData);

    void saveMergedPublishersMetadata(Integer leftId, Integer rightId, PublisherDTO leftData,
                                      PublisherDTO rightData);
}
