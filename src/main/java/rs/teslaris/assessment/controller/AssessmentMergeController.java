package rs.teslaris.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.service.interfaces.AssessmentMergeService;

@RestController
@RequestMapping("/api/assessment-merge")
@RequiredArgsConstructor
public class AssessmentMergeController {

    private final AssessmentMergeService assessmentMergeService;


    @PatchMapping("/journal-indicator/source/{sourceJournalId}/target/{targetJournalId}")
    @PreAuthorize("hasAuthority('MERGE_JOURNAL_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherJournal(@PathVariable Integer sourceJournalId,
                                                  @PathVariable Integer targetJournalId) {
        assessmentMergeService.switchAllIndicatorsToOtherJournal(sourceJournalId, targetJournalId);
    }

    @PatchMapping("/journal-classification/source/{sourceJournalId}/target/{targetJournalId}")
    @PreAuthorize("hasAuthority('MERGE_JOURNAL_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllClassificationsToOtherJournal(@PathVariable Integer sourceJournalId,
                                                       @PathVariable Integer targetJournalId) {
        assessmentMergeService.switchAllClassificationsToOtherJournal(sourceJournalId,
            targetJournalId);
    }

    @PatchMapping("/conference-indicator/source/{sourceConferenceId}/target/{targetConferenceId}")
    @PreAuthorize("hasAuthority('MERGE_CONFERENCE_PROCEEDINGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherEvent(@PathVariable Integer sourceConferenceId,
                                                @PathVariable Integer targetConferenceId) {
        assessmentMergeService.switchAllIndicatorsToOtherEvent(sourceConferenceId,
            targetConferenceId);
    }

    @PatchMapping("/conference-classification/source/{sourceConferenceId}/target/{targetConferenceId}")
    @PreAuthorize("hasAuthority('MERGE_CONFERENCE_PROCEEDINGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllClassificationsToOtherEvent(@PathVariable Integer sourceConferenceId,
                                                     @PathVariable Integer targetConferenceId) {
        assessmentMergeService.switchAllClassificationsToOtherEvent(sourceConferenceId,
            targetConferenceId);
    }
}
