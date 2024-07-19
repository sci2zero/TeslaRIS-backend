package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.merge.MergeService;

@RestController
@RequestMapping("/api/merge")
@RequiredArgsConstructor
public class MergeController {

    private final MergeService mergeService;

    @PatchMapping("/journal/{targetJournalId}/publication/{publicationId}")
    @PreAuthorize("hasAuthority('MERGE_JOURNAL_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchJournalPublicationToOtherJournal(@PathVariable Integer targetJournalId,
                                                       @PathVariable Integer publicationId) {
        mergeService.switchJournalPublicationToOtherJournal(targetJournalId, publicationId);
    }

    @PatchMapping("/journal/source/{sourceJournalId}/target/{targetJournalId}")
    @PreAuthorize("hasAuthority('MERGE_JOURNAL_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllPublicationsToOtherJournal(@PathVariable Integer sourceJournalId,
                                                    @PathVariable Integer targetJournalId) {
        mergeService.switchAllPublicationsToOtherJournal(sourceJournalId, targetJournalId);
    }

    @PatchMapping("/person/{sourcePersonId}/target/{targetPersonId}/publication/{publicationId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchPublicationToOtherPerson(@PathVariable Integer sourcePersonId,
                                               @PathVariable Integer targetPersonId,
                                               @PathVariable Integer publicationId) {
        mergeService.switchPublicationToOtherPerson(sourcePersonId, targetPersonId, publicationId);
    }

    @PatchMapping("/person/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllPublicationsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                   @PathVariable Integer targetPersonId) {
        mergeService.switchAllPublicationToOtherPerson(sourcePersonId, targetPersonId);
    }

    @PatchMapping("/employment/{sourceOUId}/target/{targetOUId}/person/{personId}")
    @PreAuthorize("hasAuthority('MERGE_OU_EMPLOYMENTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchEmployeeToOtherOrganisationUnit(@PathVariable Integer sourceOUId,
                                                      @PathVariable Integer targetOUId,
                                                      @PathVariable Integer personId) {
        mergeService.switchPersonToOtherOU(sourceOUId, targetOUId, personId);
    }

    @PatchMapping("/employment/{sourceOUId}/target/{targetOUId}")
    @PreAuthorize("hasAuthority('MERGE_OU_EMPLOYMENTS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllEmployeesToOtherOrganisationUnit(@PathVariable Integer sourceOUId,
                                                          @PathVariable Integer targetOUId) {
        mergeService.switchAllPersonsToOtherOU(sourceOUId, targetOUId);
    }

    @PatchMapping("/conference/{targetConferenceId}/proceedings/{proceedingsId}")
    @PreAuthorize("hasAuthority('MERGE_CONFERENCE_PROCEEDINGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchProceedingsToOtherConference(@PathVariable Integer targetConferenceId,
                                                   @PathVariable Integer proceedingsId) {
        mergeService.switchProceedingsToOtherConference(targetConferenceId, proceedingsId);
    }

    @PatchMapping("/conference/{sourceConferenceId}/target/{targetConferenceId}")
    @PreAuthorize("hasAuthority('MERGE_CONFERENCE_PROCEEDINGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllProceedingsToOtherConference(@PathVariable Integer sourceConferenceId,
                                                      @PathVariable Integer targetConferenceId) {
        mergeService.switchAllProceedingsToOtherConference(sourceConferenceId, targetConferenceId);
    }

    @PatchMapping("/proceedings/{targetProceedingsId}/publication/{publicationId}")
    @PreAuthorize("hasAuthority('MERGE_PROCEEDINGS_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchProceedingsPublicationToOtherProceedings(
        @PathVariable Integer targetProceedingsId,
        @PathVariable Integer publicationId) {
        mergeService.switchProceedingsPublicationToOtherProceedings(targetProceedingsId,
            publicationId);
    }

    @PatchMapping("/proceedings/{sourceProceedingsId}/target/{targetProceedingsId}")
    @PreAuthorize("hasAuthority('MERGE_PROCEEDINGS_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllProceedingsPublicationsToOtherProceedings(
        @PathVariable Integer sourceProceedingsId,
        @PathVariable Integer targetProceedingsId) {
        mergeService.switchAllPublicationsToOtherProceedings(sourceProceedingsId,
            targetProceedingsId);
    }
}
