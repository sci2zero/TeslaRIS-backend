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
    public void switchAllIndicatorsToOtherConference(@PathVariable Integer sourceConferenceId,
                                                     @PathVariable Integer targetConferenceId) {
        assessmentMergeService.switchAllIndicatorsToOtherEvent(sourceConferenceId,
            targetConferenceId);
    }

    @PatchMapping("/exhibition-indicator/source/{sourceExhibitionId}/target/{targetExhibitionId}")
    @PreAuthorize("hasAuthority('MERGE_EXHIBITIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherExhibition(@PathVariable Integer sourceExhibitionId,
                                                     @PathVariable Integer targetExhibitionId) {
        assessmentMergeService.switchAllIndicatorsToOtherEvent(sourceExhibitionId,
            targetExhibitionId);
    }

    @PatchMapping("/course-indicator/source/{sourceCourseId}/target/{targetCourseId}")
    @PreAuthorize("hasAuthority('MERGE_COURSES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherCourse(@PathVariable Integer sourceCourseId,
                                                 @PathVariable Integer targetCourseId) {
        assessmentMergeService.switchAllIndicatorsToOtherEvent(sourceCourseId,
            targetCourseId);
    }

    @PatchMapping("/other-event-indicator/source/{sourceOtherEventId}/target/{targetOtherEventId}")
    @PreAuthorize("hasAuthority('MERGE_OTHER_EVENTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllIndicatorsToOtherOtherEVent(@PathVariable Integer sourceOtherEventId,
                                                     @PathVariable Integer targetOtherEVentId) {
        assessmentMergeService.switchAllIndicatorsToOtherEvent(sourceOtherEventId,
            targetOtherEVentId);
    }

    @PatchMapping("/conference-classification/source/{sourceConferenceId}/target/{targetConferenceId}")
    @PreAuthorize("hasAuthority('MERGE_CONFERENCE_PROCEEDINGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllClassificationsToOtherConference(@PathVariable Integer sourceConferenceId,
                                                          @PathVariable
                                                          Integer targetConferenceId) {
        assessmentMergeService.switchAllClassificationsToOtherEvent(sourceConferenceId,
            targetConferenceId);
    }

    @PatchMapping("/exhibition-classification/source/{sourceExhibitionId}/target/{targetExhibitionId}")
    @PreAuthorize("hasAuthority('MERGE_EXHIBITIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllClassificationsToOtherExhibition(@PathVariable Integer sourceExhibitionId,
                                                          @PathVariable
                                                          Integer targetExhibitionId) {
        assessmentMergeService.switchAllClassificationsToOtherEvent(sourceExhibitionId,
            targetExhibitionId);
    }

    @PatchMapping("/course-classification/source/{sourceCourseId}/target/{targetCourseId}")
    @PreAuthorize("hasAuthority('MERGE_COURSES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllClassificationsToOtherCourse(@PathVariable Integer sourceCourseId,
                                                      @PathVariable
                                                      Integer targetCourseId) {
        assessmentMergeService.switchAllClassificationsToOtherEvent(sourceCourseId, targetCourseId);
    }

    @PatchMapping("/other-event-classification/source/{sourceOtherEventId}/target/{targetOtherEventId}")
    @PreAuthorize("hasAuthority('MERGE_OTHER_EVENTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllClassificationsToOtherOtherEvent(@PathVariable Integer sourceOtherEventId,
                                                          @PathVariable
                                                          Integer targetOtherEventId) {
        assessmentMergeService.switchAllClassificationsToOtherEvent(sourceOtherEventId,
            targetOtherEventId);
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
