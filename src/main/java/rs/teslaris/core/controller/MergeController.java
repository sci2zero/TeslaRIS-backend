package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.deduplication.MergedBookSeriesDTO;
import rs.teslaris.core.dto.deduplication.MergedConferenceDTO;
import rs.teslaris.core.dto.deduplication.MergedDatasetsDTO;
import rs.teslaris.core.dto.deduplication.MergedDocumentsDTO;
import rs.teslaris.core.dto.deduplication.MergedJournalPublicationsDTO;
import rs.teslaris.core.dto.deduplication.MergedJournalsDTO;
import rs.teslaris.core.dto.deduplication.MergedMonographPublicationsDTO;
import rs.teslaris.core.dto.deduplication.MergedMonographsDTO;
import rs.teslaris.core.dto.deduplication.MergedOrganisationUnitsDTO;
import rs.teslaris.core.dto.deduplication.MergedPatentsDTO;
import rs.teslaris.core.dto.deduplication.MergedPersonsDTO;
import rs.teslaris.core.dto.deduplication.MergedProceedingsDTO;
import rs.teslaris.core.dto.deduplication.MergedProceedingsPublicationsDTO;
import rs.teslaris.core.dto.deduplication.MergedPublishersDTO;
import rs.teslaris.core.dto.deduplication.MergedSoftwareDTO;
import rs.teslaris.core.dto.deduplication.MergedThesesDTO;
import rs.teslaris.core.dto.person.involvement.PersonCollectionEntitySwitchListDTO;
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

    @PatchMapping("/publisher/{targetPublisherId}/publication/{publicationId}")
    @PreAuthorize("hasAuthority('MERGE_PUBLISHER_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchPublisherPublicationToOtherPublisher(@PathVariable Integer targetPublisherId,
                                                           @PathVariable Integer publicationId) {
        mergeService.switchPublisherPublicationToOtherPublisher(targetPublisherId, publicationId);
    }

    @PatchMapping("/publisher/source/{sourcePublisherId}/target/{targetPublisherId}")
    @PreAuthorize("hasAuthority('MERGE_PUBLISHER_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllPublicationsToOtherPublisher(@PathVariable Integer sourcePublisherId,
                                                      @PathVariable Integer targetPublisherId) {
        mergeService.switchAllPublicationsToOtherPublisher(sourcePublisherId, targetPublisherId);
    }

    @PatchMapping("/book-series/{targetBookSeriesId}/publication/{publicationId}")
    @PreAuthorize("hasAuthority('MERGE_BOOK_SERIES_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchPublicationToOtherBookSeries(@PathVariable Integer targetBookSeriesId,
                                                   @PathVariable Integer publicationId) {
        mergeService.switchPublicationToOtherBookSeries(targetBookSeriesId, publicationId);
    }

    @PatchMapping("/book-series/source/{sourceBookSeriesId}/target/{targetBookSeriesId}")
    @PreAuthorize("hasAuthority('MERGE_BOOK_SERIES_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllPublicationsToOtherBookSeries(@PathVariable Integer sourceBookSeriesId,
                                                       @PathVariable Integer targetBookSeriesId) {
        mergeService.switchAllPublicationsToOtherBookSeries(sourceBookSeriesId, targetBookSeriesId);
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

    @PatchMapping("/organisation-unit/metadata/{leftOrganisationUnitId}/{rightOrganisationUnitId}")
    @PreAuthorize("hasAuthority('MERGE_OU_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedOrganisationUnitsMetadata(
        @PathVariable Integer leftOrganisationUnitId,
        @PathVariable Integer rightOrganisationUnitId,
        @NotNull @RequestBody MergedOrganisationUnitsDTO mergedOrganisationUnits) {
        mergeService.saveMergedOUsMetadata(leftOrganisationUnitId, rightOrganisationUnitId,
            mergedOrganisationUnits.getLeftOrganisationUnit(),
            mergedOrganisationUnits.getRightOrganisationUnit());
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

    @PatchMapping("/monograph/{targetMonographId}/publication/{publicationId}")
    @PreAuthorize("hasAuthority('MERGE_MONOGRAPH_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchMonographPublicationToOtherMonograph(@PathVariable Integer targetMonographId,
                                                           @PathVariable Integer publicationId) {
        mergeService.switchPublicationToOtherMonograph(targetMonographId, publicationId);
    }

    @PatchMapping("/monograph/source/{sourceMonographId}/target/{targetMonographId}")
    @PreAuthorize("hasAuthority('MERGE_MONOGRAPH_PUBLICATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllPublicationsToOtherMonograph(@PathVariable Integer sourceMonographId,
                                                      @PathVariable Integer targetMonographId) {
        mergeService.switchAllPublicationsToOtherMonograph(sourceMonographId, targetMonographId);
    }

    @PatchMapping("/person/involvements/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    public void switchInvolvementsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                @PathVariable Integer targetPersonId,
                                                @NotNull @RequestBody
                                                PersonCollectionEntitySwitchListDTO involvementSwitchList) {
        mergeService.switchInvolvements(involvementSwitchList.getEntityIds(),
            sourcePersonId, targetPersonId);
    }

    @PatchMapping("/person/skills/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    public void switchSkillsToOtherPerson(@PathVariable Integer sourcePersonId,
                                          @PathVariable Integer targetPersonId,
                                          @NotNull @RequestBody
                                          PersonCollectionEntitySwitchListDTO skillSwitchList) {
        mergeService.switchSkills(skillSwitchList.getEntityIds(), sourcePersonId,
            targetPersonId);
    }

    @PatchMapping("/person/prizes/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    public void switchPrizesToOtherPerson(@PathVariable Integer sourcePersonId,
                                          @PathVariable Integer targetPersonId,
                                          @NotNull @RequestBody
                                          PersonCollectionEntitySwitchListDTO prizeSwitchList) {
        mergeService.switchPrizes(prizeSwitchList.getEntityIds(), sourcePersonId, targetPersonId);
    }

    @PatchMapping("/proceedings/metadata/{leftProceedingsId}/{rightProceedingsId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedProceedingsMetadata(
        @PathVariable Integer leftProceedingsId,
        @PathVariable Integer rightProceedingsId,
        @NotNull @RequestBody MergedProceedingsDTO mergedProceedings) {
        mergeService.saveMergedProceedingsMetadata(leftProceedingsId, rightProceedingsId,
            mergedProceedings.getLeftProceedings(), mergedProceedings.getRightProceedings());

        mergeDocumentFiles(leftProceedingsId, rightProceedingsId, mergedProceedings);
    }

    @PatchMapping("/person/metadata/{leftPersonId}/{rightPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedPersonsMetadata(
        @PathVariable Integer leftPersonId,
        @PathVariable Integer rightPersonId,
        @NotNull @RequestBody MergedPersonsDTO mergedPersons) {
        mergeService.saveMergedPersonsMetadata(leftPersonId, rightPersonId,
            mergedPersons.getLeftPerson(), mergedPersons.getRightPerson());
    }

    @PatchMapping("/conference/metadata/{leftConferenceId}/{rightConferenceId}")
    @PreAuthorize("hasAuthority('MERGE_EVENT_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedConferencesMetadata(
        @PathVariable Integer leftConferenceId,
        @PathVariable Integer rightConferenceId,
        @NotNull @RequestBody MergedConferenceDTO mergedConference) {
        mergeService.saveMergedConferencesMetadata(leftConferenceId, rightConferenceId,
            mergedConference.getLeftConference(), mergedConference.getRightConference());
    }

    @PatchMapping("/journal/metadata/{leftJournalId}/{rightJournalId}")
    @PreAuthorize("hasAuthority('MERGE_PUBLICATION_SERIES_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedJournalsMetadata(
        @PathVariable Integer leftJournalId,
        @PathVariable Integer rightJournalId,
        @NotNull @RequestBody MergedJournalsDTO mergedJournals) {
        mergeService.saveMergedJournalsMetadata(leftJournalId, rightJournalId,
            mergedJournals.getLeftJournal(), mergedJournals.getRightJournal());
    }

    @PatchMapping("/book-series/metadata/{leftBookSeriesId}/{rightBookSeriesId}")
    @PreAuthorize("hasAuthority('MERGE_PUBLICATION_SERIES_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedJournalsMetadata(
        @PathVariable Integer leftBookSeriesId,
        @PathVariable Integer rightBookSeriesId,
        @NotNull @RequestBody MergedBookSeriesDTO mergedBookSeries) {
        mergeService.saveMergedBookSeriesMetadata(leftBookSeriesId, rightBookSeriesId,
            mergedBookSeries.getLeftBookSeries(), mergedBookSeries.getRightBookSeries());
    }

    @PatchMapping("/software/metadata/{leftSoftwareId}/{rightSoftwareId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedSoftwareMetadata(
        @PathVariable Integer leftSoftwareId,
        @PathVariable Integer rightSoftwareId,
        @NotNull @RequestBody MergedSoftwareDTO mergedSoftware) {
        mergeService.saveMergedSoftwareMetadata(leftSoftwareId, rightSoftwareId,
            mergedSoftware.getLeftSoftware(), mergedSoftware.getRightSoftware());

        mergeDocumentFiles(leftSoftwareId, rightSoftwareId, mergedSoftware);
    }

    @PatchMapping("/dataset/metadata/{leftDatasetId}/{rightDatasetId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedDatasetsMetadata(
        @PathVariable Integer leftDatasetId,
        @PathVariable Integer rightDatasetId,
        @NotNull @RequestBody MergedDatasetsDTO mergedDatasets) {
        mergeService.saveMergedDatasetsMetadata(leftDatasetId, rightDatasetId,
            mergedDatasets.getLeftDataset(), mergedDatasets.getRightDataset());

        mergeDocumentFiles(leftDatasetId, rightDatasetId, mergedDatasets);
    }

    @PatchMapping("/patent/metadata/{leftPatentId}/{rightPatentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedPatentsMetadata(
        @PathVariable Integer leftPatentId,
        @PathVariable Integer rightPatentId,
        @NotNull @RequestBody MergedPatentsDTO mergedPatents) {
        mergeService.saveMergedPatentsMetadata(leftPatentId, rightPatentId,
            mergedPatents.getLeftPatent(), mergedPatents.getRightPatent());

        mergeDocumentFiles(leftPatentId, rightPatentId, mergedPatents);
    }

    @PatchMapping("/proceedings-publication/metadata/{leftProceedingsPublicationId}/{rightProceedingsPublicationId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedProceedingsPublicationsMetadata(
        @PathVariable Integer leftProceedingsPublicationId,
        @PathVariable Integer rightProceedingsPublicationId,
        @NotNull @RequestBody MergedProceedingsPublicationsDTO mergedProceedingsPublications) {
        mergeService.saveMergedProceedingsPublicationMetadata(leftProceedingsPublicationId,
            rightProceedingsPublicationId,
            mergedProceedingsPublications.getLeftProceedingsPublication(),
            mergedProceedingsPublications.getRightProceedingsPublication());

        mergeDocumentFiles(leftProceedingsPublicationId, rightProceedingsPublicationId,
            mergedProceedingsPublications);
    }

    @PatchMapping("/thesis/metadata/{leftThesisId}/{rightThesisId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedThesesMetadata(
        @PathVariable Integer leftThesisId,
        @PathVariable Integer rightThesisId,
        @NotNull @RequestBody MergedThesesDTO mergedTheses) {
        mergeService.saveMergedThesesMetadata(leftThesisId, rightThesisId,
            mergedTheses.getLeftThesis(), mergedTheses.getRightThesis());

        mergeDocumentFiles(leftThesisId, rightThesisId,
            mergedTheses);
    }

    @PatchMapping("/journal-publication/metadata/{leftJournalPublicationId}/{rightJournalPublicationId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedProceedingsPublicationsMetadata(
        @PathVariable Integer leftJournalPublicationId,
        @PathVariable Integer rightJournalPublicationId,
        @NotNull @RequestBody MergedJournalPublicationsDTO mergedJournalPublications) {
        mergeService.saveMergedJournalPublicationMetadata(leftJournalPublicationId,
            rightJournalPublicationId,
            mergedJournalPublications.getLeftJournalPublication(),
            mergedJournalPublications.getRightJournalPublication());

        mergeDocumentFiles(leftJournalPublicationId, rightJournalPublicationId,
            mergedJournalPublications);
    }

    @PatchMapping("/monograph/metadata/{leftMonographId}/{rightMonographId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedMonographsMetadata(@PathVariable Integer leftMonographId,
                                             @PathVariable Integer rightMonographId,
                                             @NotNull @RequestBody
                                             MergedMonographsDTO mergedMonographs) {
        mergeService.saveMergedMonographsMetadata(leftMonographId,
            rightMonographId,
            mergedMonographs.getLeftMonograph(),
            mergedMonographs.getRightMonograph());

        mergeDocumentFiles(leftMonographId, rightMonographId,
            mergedMonographs);
    }

    @PatchMapping("/monograph-publication/metadata/{leftMonographPublicationId}/{rightMonographPublicationId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedMonographPublicationsMetadata(
        @PathVariable Integer leftMonographPublicationId,
        @PathVariable Integer rightMonographPublicationId,
        @NotNull @RequestBody MergedMonographPublicationsDTO mergedMonographPublications) {
        mergeService.saveMergedMonographPublicationsMetadata(leftMonographPublicationId,
            rightMonographPublicationId,
            mergedMonographPublications.getLeftMonographPublication(),
            mergedMonographPublications.getRightMonographPublication());

        mergeDocumentFiles(leftMonographPublicationId, rightMonographPublicationId,
            mergedMonographPublications);
    }

    @PatchMapping("/publisher/metadata/{leftPublisherId}/{rightPublisherId}")
    @PreAuthorize("hasAuthority('MERGE_PUBLISHERS_METADATA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedPublishersMetadata(
        @PathVariable Integer leftPublisherId,
        @PathVariable Integer rightPublisherId,
        @NotNull @RequestBody MergedPublishersDTO mergedPublishers) {
        mergeService.saveMergedPublishersMetadata(leftPublisherId,
            rightPublisherId,
            mergedPublishers.getLeftPublisher(),
            mergedPublishers.getRightPublisher());
    }

    private void mergeDocumentFiles(Integer leftId, Integer rightId,
                                    MergedDocumentsDTO mergedDocuments) {
        mergeService.saveMergedDocumentFiles(leftId, rightId,
            mergedDocuments.getLeftProofs(), mergedDocuments.getRightProofs(),
            mergedDocuments.getLeftFileItems(), mergedDocuments.getRightFileItems());
    }
}
