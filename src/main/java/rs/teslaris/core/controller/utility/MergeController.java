package rs.teslaris.core.controller.utility;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.annotation.PublicationMergeCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.deduplication.MergedBookSeriesDTO;
import rs.teslaris.core.dto.deduplication.MergedConferenceDTO;
import rs.teslaris.core.dto.deduplication.MergedDatasetsDTO;
import rs.teslaris.core.dto.deduplication.MergedDocumentsDTO;
import rs.teslaris.core.dto.deduplication.MergedGeneticMaterialDTO;
import rs.teslaris.core.dto.deduplication.MergedJournalPublicationsDTO;
import rs.teslaris.core.dto.deduplication.MergedJournalsDTO;
import rs.teslaris.core.dto.deduplication.MergedMaterialProductDTO;
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
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.merge.MergeService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/merge")
@RequiredArgsConstructor
@Traceable
public class MergeController {

    private final MergeService mergeService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


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
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchPublicationToOtherPerson(@PathVariable Integer sourcePersonId,
                                               @PathVariable Integer targetPersonId,
                                               @PathVariable Integer publicationId) {
        mergeService.switchPublicationToOtherPerson(sourcePersonId, targetPersonId, publicationId);
    }

    @PatchMapping("/person/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_PUBLICATIONS')")
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchAllPublicationsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                   @PathVariable Integer targetPersonId,
                                                   @RequestParam(value = "contributionType", required = false, defaultValue = "AUTHOR")
                                                   DocumentContributionType contributionType) {
        mergeService.switchAllPublicationToOtherPerson(sourcePersonId, targetPersonId,
            contributionType);
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
    @OrgUnitEditCheck
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
    @PersonEditCheck
    public void switchInvolvementsToOtherPerson(@PathVariable Integer sourcePersonId,
                                                @PathVariable Integer targetPersonId,
                                                @NotNull @RequestBody
                                                PersonCollectionEntitySwitchListDTO involvementSwitchList,
                                                @RequestHeader("Authorization")
                                                String bearerToken) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken)
            .equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            var institutionId = userService.getUserOrganisationUnitId(
                tokenUtil.extractUserIdFromToken(bearerToken));
            mergeService.switchInvolvements(involvementSwitchList.getEntityIds(),
                sourcePersonId, targetPersonId, institutionId);
            return;
        }

        mergeService.switchInvolvements(involvementSwitchList.getEntityIds(),
            sourcePersonId, targetPersonId, null);
    }

    @PatchMapping("/person/skills/source/{sourcePersonId}/target/{targetPersonId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    @PersonEditCheck
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

    @PatchMapping("/proceedings/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedProceedingsMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedProceedingsDTO mergedProceedings) {
        mergeService.saveMergedProceedingsMetadata(leftDocumentId, rightDocumentId,
            mergedProceedings.getLeftProceedings(), mergedProceedings.getRightProceedings());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedProceedings);
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

    @PatchMapping("/software/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedSoftwareMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedSoftwareDTO mergedSoftware) {
        mergeService.saveMergedSoftwareMetadata(leftDocumentId, rightDocumentId,
            mergedSoftware.getLeftSoftware(), mergedSoftware.getRightSoftware());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedSoftware);
    }

    @PatchMapping("/material-product/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedMaterialProductMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedMaterialProductDTO mergedMaterialProduct) {
        mergeService.saveMergedMaterialProductMetadata(leftDocumentId, rightDocumentId,
            mergedMaterialProduct.getLeftMaterialProduct(),
            mergedMaterialProduct.getRightMaterialProduct());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedMaterialProduct);
    }

    @PatchMapping("/genetic-material/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedGeneticMaterialMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedGeneticMaterialDTO mergedGeneticMaterial) {
        mergeService.saveMergedGeneticMaterialMetadata(leftDocumentId, rightDocumentId,
            mergedGeneticMaterial.getLeftGeneticMaterial(),
            mergedGeneticMaterial.getRightGeneticMaterial());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedGeneticMaterial);
    }

    @PatchMapping("/dataset/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedDatasetsMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedDatasetsDTO mergedDatasets) {
        mergeService.saveMergedDatasetsMetadata(leftDocumentId, rightDocumentId,
            mergedDatasets.getLeftDataset(), mergedDatasets.getRightDataset());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedDatasets);
    }

    @PatchMapping("/patent/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedPatentsMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedPatentsDTO mergedPatents) {
        mergeService.saveMergedPatentsMetadata(leftDocumentId, rightDocumentId,
            mergedPatents.getLeftPatent(), mergedPatents.getRightPatent());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedPatents);
    }

    @PatchMapping("/proceedings-publication/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedProceedingsPublicationsMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedProceedingsPublicationsDTO mergedProceedingsPublications) {
        mergeService.saveMergedProceedingsPublicationMetadata(leftDocumentId, rightDocumentId,
            mergedProceedingsPublications.getLeftProceedingsPublication(),
            mergedProceedingsPublications.getRightProceedingsPublication());

        mergeDocumentFiles(leftDocumentId, rightDocumentId,
            mergedProceedingsPublications);
    }

    @PatchMapping("/thesis/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedThesesMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedThesesDTO mergedTheses) {
        mergeService.saveMergedThesesMetadata(leftDocumentId, rightDocumentId,
            mergedTheses.getLeftThesis(), mergedTheses.getRightThesis());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedTheses);
    }

    @PatchMapping("/journal-publication/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedProceedingsPublicationsMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedJournalPublicationsDTO mergedJournalPublications) {
        mergeService.saveMergedJournalPublicationMetadata(leftDocumentId, rightDocumentId,
            mergedJournalPublications.getLeftJournalPublication(),
            mergedJournalPublications.getRightJournalPublication());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedJournalPublications);
    }

    @PatchMapping("/monograph/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedMonographsMetadata(@PathVariable Integer leftDocumentId,
                                             @PathVariable Integer rightDocumentId,
                                             @NotNull @RequestBody
                                             MergedMonographsDTO mergedMonographs) {
        mergeService.saveMergedMonographsMetadata(leftDocumentId, rightDocumentId,
            mergedMonographs.getLeftMonograph(), mergedMonographs.getRightMonograph());

        mergeDocumentFiles(leftDocumentId, rightDocumentId, mergedMonographs);
    }

    @PatchMapping("/monograph-publication/metadata/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAuthority('MERGE_DOCUMENTS_METADATA')")
    @PublicationMergeCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMergedMonographPublicationsMetadata(
        @PathVariable Integer leftDocumentId,
        @PathVariable Integer rightDocumentId,
        @NotNull @RequestBody MergedMonographPublicationsDTO mergedMonographPublications) {
        mergeService.saveMergedMonographPublicationsMetadata(leftDocumentId, rightDocumentId,
            mergedMonographPublications.getLeftMonographPublication(),
            mergedMonographPublications.getRightMonographPublication());

        mergeDocumentFiles(leftDocumentId, rightDocumentId,
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

    @PatchMapping("/migrate-identifier-history/generic/{entityType}/{deletionEntityId}/{mergedEntityId}")
    @PreAuthorize("hasAuthority('MIGRATE_ALL_ENTITIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void migrateIdentifierHistory(@PathVariable EntityType entityType,
                                         @PathVariable Integer deletionEntityId,
                                         @PathVariable Integer mergedEntityId) {
        mergeService.migratePersistentIdentifiers(deletionEntityId, mergedEntityId, entityType);
    }

    @PatchMapping("/migrate-identifier-history/publication/{publicationType}/{leftDocumentId}/{rightDocumentId}")
    @PreAuthorize("hasAnyAuthority('MIGRATE_ALL_ENTITIES', 'MIGRATE_INSTITUTION_ENTITIES')")
    @PublicationMergeCheck("MERGE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void migratePublicationIdentifierHistory(@PathVariable String publicationType,
                                                    @PathVariable("leftDocumentId")
                                                    Integer deletionEntityId,
                                                    @PathVariable("rightDocumentId")
                                                    Integer mergedEntityId) {
        var entityType = EntityType.PUBLICATION;
        if (publicationType.equals("monograph")) {
            entityType = EntityType.MONOGRAPH;
        } else if (publicationType.equals("proceedings")) {
            entityType = EntityType.PROCEEDINGS;
        }

        mergeService.migratePersistentIdentifiers(deletionEntityId, mergedEntityId, entityType);
    }

    @PatchMapping("/migrate-identifier-history/person/{sourcePersonId}/{targetPersonId}")
    @PreAuthorize("hasAnyAuthority('MIGRATE_ALL_ENTITIES', 'MIGRATE_INSTITUTION_ENTITIES')")
    @PersonEditCheck("MERGE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void migratePersonIdentifierHistory(
        @PathVariable("sourcePersonId") Integer deletionEntityId,
        @PathVariable("targetPersonId") Integer mergedEntityId) {
        mergeService.migratePersistentIdentifiers(deletionEntityId, mergedEntityId,
            EntityType.PERSON);
    }

    @PatchMapping("/migrate-identifier-history/organisation-unit/{leftOrganisationUnitId}/{rightOrganisationUnitId}")
    @PreAuthorize("hasAnyAuthority('MIGRATE_ALL_ENTITIES', 'MIGRATE_INSTITUTION_ENTITIES')")
    @OrgUnitEditCheck("MERGE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void migrateOrganisationUnitIdentifierHistory(
        @PathVariable("leftOrganisationUnitId") Integer deletionEntityId,
        @PathVariable("rightOrganisationUnitId") Integer mergedEntityId) {
        mergeService.migratePersistentIdentifiers(deletionEntityId, mergedEntityId,
            EntityType.ORGANISATION_UNIT);
    }

    private void mergeDocumentFiles(Integer leftId, Integer rightId,
                                    MergedDocumentsDTO mergedDocuments) {
        mergeService.saveMergedDocumentFiles(leftId, rightId,
            mergedDocuments.getLeftProofs(), mergedDocuments.getRightProofs(),
            mergedDocuments.getLeftFileItems(), mergedDocuments.getRightFileItems());
    }
}
