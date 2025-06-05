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
import rs.teslaris.core.annotation.Traceable;

@RestController
@RequestMapping("/api/assessment-merge")
@RequiredArgsConstructor
@Traceable
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

    @PatchMapping("/book-series-indicator/source/{sourceBookSeriesId}/target/{targetBookSeriesId}")
    @PreAuthorize("hasAuthority('MERGE_BOOK_SERIES_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherBookSeries(@PathVariable Integer sourceBookSeriesId,
                                                     @PathVariable Integer targetBookSeriesId) {
        assessmentMergeService.switchAllIndicatorsToOtherBookSeries(sourceBookSeriesId,
            targetBookSeriesId);
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

    @PatchMapping("/person-indicator/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                 @PathVariable Integer targetPersonId) {
        assessmentMergeService.switchAllIndicatorsToOtherPerson(sourcePersonId,
            targetPersonId);
    }

    @PatchMapping("/organisation-unit-indicator/source/{sourceOrganisationUnitId}/target/{targetOrganisationUnitId}")
    @PreAuthorize("hasAuthority('MERGE_OU_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherOrganisationUnit(
        @PathVariable Integer sourceOrganisationUnitId,
        @PathVariable Integer targetOrganisationUnitId) {
        assessmentMergeService.switchAllIndicatorsToOtherOrganisationUnit(sourceOrganisationUnitId,
            targetOrganisationUnitId);
    }

    @PatchMapping("/document-indicator/source/{sourceDocumentId}/target/{targetDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherDocument(@PathVariable Integer sourceDocumentId,
                                                   @PathVariable Integer targetDocumentId) {
        assessmentMergeService.switchAllIndicatorsToOtherDocument(sourceDocumentId,
            targetDocumentId);
    }
}
