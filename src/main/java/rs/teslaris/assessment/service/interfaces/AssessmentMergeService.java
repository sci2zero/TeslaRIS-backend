package rs.teslaris.assessment.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface AssessmentMergeService {

    void switchAllIndicatorsToOtherJournal(Integer sourceId, Integer targetId);

    void switchAllClassificationsToOtherJournal(Integer sourceId, Integer targetId);

    void switchAllIndicatorsToOtherEvent(Integer sourceId, Integer targetId);

    void switchAllClassificationsToOtherEvent(Integer sourceId, Integer targetId);

    void switchAllIndicatorsToOtherPerson(Integer sourceId, Integer targetId);

    void switchAllIndicatorsToOtherOrganisationUnit(Integer sourceId, Integer targetId);

    void switchAllIndicatorsToOtherDocument(Integer sourceId, Integer targetId);

    void switchAllIndicatorsToOtherBookSeries(Integer sourceId, Integer targetId);
}
