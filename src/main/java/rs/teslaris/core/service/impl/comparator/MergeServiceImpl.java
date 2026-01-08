package rs.teslaris.core.service.impl.comparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.OrganisationUnitSignificantChangeEvent;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.BookSeriesPublishable;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.PublisherPublishable;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.SoftwareRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.GeneticMaterialService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.merge.MergeService;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.Accounted;
import rs.teslaris.core.util.deduplication.Mergeable;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class MergeServiceImpl implements MergeService {

    private final JournalService journalService;

    private final JournalPublicationService journalPublicationService;

    private final JournalPublicationRepository journalPublicationRepository;

    private final ProceedingsPublicationService proceedingsPublicationService;

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentRepository documentRepository;

    private final PersonService personService;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final ConferenceService conferenceService;

    private final ProceedingsService proceedingsService;

    private final PrizeService prizeService;

    private final ExpertiseOrSkillService expertiseOrSkillService;

    private final InvolvementService involvementService;

    private final SoftwareService softwareService;

    private final MaterialProductService materialProductService;

    private final GeneticMaterialService geneticMaterialService;

    private final DatasetService datasetService;

    private final PatentService patentService;

    private final ThesisService thesisService;

    private final MonographService monographService;

    private final MonographPublicationService monographPublicationService;

    private final ProceedingsRepository proceedingsRepository;

    private final MonographRepository monographRepository;

    private final BookSeriesService bookSeriesService;

    private final SoftwareRepository softwareRepository;

    private final DatasetRepository datasetRepository;

    private final PatentRepository patentRepository;

    private final ThesisRepository thesisRepository;

    private final PublisherService publisherService;

    private final IndexBulkUpdateService indexBulkUpdateService;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    public void switchJournalPublicationToOtherJournal(Integer targetJournalId,
                                                       Integer publicationId) {
        performJournalPublicationSwitch(targetJournalId, publicationId);
    }

    @Override
    public void switchPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId,
                                               Integer publicationId) {
        performPersonPublicationSwitch(sourcePersonId, targetPersonId, publicationId, false);
    }

    @Override
    public void switchAllPublicationsToOtherJournal(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, journalPublicationIndex) -> performJournalPublicationSwitch(targetId,
                journalPublicationIndex.getDatabaseId()),
            pageRequest -> documentPublicationIndexRepository.findByTypeAndJournalId(
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, pageRequest)
                .getContent()
        );
    }

    @Override
    public void switchPublisherPublicationToOtherPublisher(Integer targetPublisherId,
                                                           Integer publicationId) {
        performPublisherPublicationSwitch(targetPublisherId, publicationId);
    }

    @Override
    public void switchAllPublicationsToOtherPublisher(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, documentIndex) -> performPublisherPublicationSwitch(targetId,
                documentIndex.getDatabaseId()),
            pageRequest -> documentPublicationIndexRepository.findByPublisherId(sourceId,
                pageRequest).getContent()
        );
    }

    @Override
    public void switchPublicationToOtherBookSeries(Integer targetBookSeriesId,
                                                   Integer publicationId) {
        performBookSeriesPublicationSwitch(targetBookSeriesId, publicationId);
    }

    @Override
    public void switchAllPublicationsToOtherBookSeries(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, publicationIndex) -> performBookSeriesPublicationSwitch(targetId,
                publicationIndex.getDatabaseId()),
            pageRequest -> documentPublicationIndexRepository.findByTypeInAndPublicationSeriesId(
                    List.of(DocumentPublicationType.MONOGRAPH.name(),
                        DocumentPublicationType.PROCEEDINGS.name()), sourceId, pageRequest)
                .getContent()
        );
    }

    @Override
    public void switchProceedingsPublicationToOtherProceedings(Integer targetProceedingsId,
                                                               Integer publicationId) {
        performProceedingsPublicationSwitch(targetProceedingsId, publicationId);
    }

    @Override
    public void switchAllPublicationsToOtherProceedings(Integer sourceId, Integer targetId) {
        processChunks(
            sourceId,
            (srcId, proceedingsPublicationIndex) -> performProceedingsPublicationSwitch(targetId,
                proceedingsPublicationIndex.getDatabaseId()),
            pageRequest -> documentPublicationIndexRepository.findByTypeAndProceedingsId(
                    DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(), sourceId, pageRequest)
                .getContent()
        );
    }

    @Override
    public void switchAllPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId,
                                                  DocumentContributionType contributionType) {
        processChunks(
            sourcePersonId,
            (srcId, personPublicationIndex) -> performPersonPublicationSwitch(srcId, targetPersonId,
                personPublicationIndex.getDatabaseId(), true),
            pageRequest ->
                documentPublicationService.findResearcherPublications(sourcePersonId,
                    Collections.emptyList(), List.of("*"),
                    Arrays.asList(DocumentPublicationType.values()), contributionType,
                    pageRequest).getContent()
        );
    }

    @Override
    public void switchPersonToOtherOU(Integer sourceOUId, Integer targetOUId, Integer personId) {
        performEmployeeSwitch(sourceOUId, targetOUId, personId, true);
    }

    @Override
    public void switchAllPersonsToOtherOU(Integer sourceOUId, Integer targetOUId) {
        processChunks(
            sourceOUId,
            (srcId, personIndex) -> performEmployeeSwitch(srcId, targetOUId,
                personIndex.getDatabaseId(), false),
            pageRequest -> personService.findPeopleForOrganisationUnit(sourceOUId, List.of("*"),
                    pageRequest,
                    false)
                .getContent()
        );

        // Bulk reindex bound entities (persons + publications)
        applicationEventPublisher.publishEvent(
            new OrganisationUnitSignificantChangeEvent(targetOUId));
    }

    @Override
    public void switchProceedingsToOtherConference(Integer targetConferenceId,
                                                   Integer proceedingsId) {
        performProceedingsSwitch(targetConferenceId, proceedingsId);
    }

    @Override
    public void switchAllProceedingsToOtherConference(Integer sourceConferenceId,
                                                      Integer targetConferenceId) {
        processChunks(
            sourceConferenceId,
            (srcId, proceedingsResponse) -> performProceedingsSwitch(targetConferenceId,
                proceedingsResponse.getId()),
            pageRequest -> proceedingsService.readProceedingsForEventId(sourceConferenceId)
        );
    }

    @Override
    public void switchInvolvements(List<Integer> involvementIds, Integer sourcePersonId,
                                   Integer targetPersonId, Integer institutionId) {
        var sourcePerson = personService.findOne(sourcePersonId);
        var targetPerson = personService.findOne(targetPersonId);

        var institutionIds = new ArrayList<Integer>();
        if (Objects.nonNull(institutionId)) {
            institutionIds.addAll(
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId));
        }

        involvementIds.forEach(involvementId -> {
            var involvementToUpdate = involvementService.findOne(involvementId);

            if (!institutionIds.isEmpty() && (
                involvementToUpdate.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                    involvementToUpdate.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                institutionIds.contains(involvementToUpdate.getOrganisationUnit().getId())) {
                return;
            }

            if (sourcePerson.getInvolvements().contains(involvementToUpdate)) {
                sourcePerson.removeInvolvement(involvementToUpdate);
            }

            if (!targetPerson.getInvolvements().contains(involvementToUpdate)) {
                involvementToUpdate.setPersonInvolved(targetPerson);
                targetPerson.addInvolvement(involvementToUpdate);
            }

            involvementService.save(involvementToUpdate);
        });

        personService.save(sourcePerson);
        personService.save(targetPerson);

        userService.updateResearcherCurrentOrganisationUnitIfBound(sourcePersonId);
        userService.updateResearcherCurrentOrganisationUnitIfBound(targetPersonId);

        personService.indexPerson(sourcePerson);
        personService.indexPerson(targetPerson);
    }

    @Override
    public void switchSkills(List<Integer> skillIds, Integer sourcePersonId,
                             Integer targetPersonId) {
        var sourcePerson = personService.findOne(sourcePersonId);
        var targetPerson = personService.findOne(targetPersonId);

        skillIds.forEach(skillId -> {
            var skillToUpdate = expertiseOrSkillService.findOne(skillId);

            sourcePerson.getExpertisesAndSkills().remove(skillToUpdate);

            if (!targetPerson.getExpertisesAndSkills().contains(skillToUpdate)) {
                targetPerson.getExpertisesAndSkills().add(skillToUpdate);
                skillToUpdate.setPerson(targetPerson);
            }

            expertiseOrSkillService.save(skillToUpdate);
        });
    }

    @Override
    public void switchPrizes(List<Integer> prizeIds, Integer sourcePersonId,
                             Integer targetPersonId) {
        var sourcePerson = personService.findOne(sourcePersonId);
        var targetPerson = personService.findOne(targetPersonId);

        prizeIds.forEach(prizeId -> {
            var prizeToUpdate = prizeService.findOne(prizeId);

            sourcePerson.getPrizes().remove(prizeToUpdate);

            if (!targetPerson.getPrizes().contains(prizeToUpdate)) {
                targetPerson.addPrize(prizeToUpdate);
            }

            prizeService.save(prizeToUpdate);
        });
    }

    @Override
    public void saveMergedDocumentFiles(Integer leftId, Integer rightId,
                                        List<Integer> leftProofs,
                                        List<Integer> rightProofs,
                                        List<Integer> leftFileItems,
                                        List<Integer> rightFileItems) {

        var leftDocument = documentPublicationService.findDocumentById(leftId);
        var rightDocument = documentPublicationService.findDocumentById(rightId);

        // Merge proofs
        mergeDocumentFiles(leftDocument, rightDocument, leftProofs, rightProofs);

        // Merge fileItems
        mergeDocumentFiles(leftDocument, rightDocument, leftFileItems, rightFileItems);
    }

    @Override
    public void saveMergedProceedingsMetadata(Integer leftId, Integer rightId,
                                              ProceedingsDTO leftData, ProceedingsDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(proceedingsService::updateProceedings,
            proceedingsService::indexProceedings, proceedingsService::findProceedingsById, leftId,
            rightId, leftData,
            rightData,
            dto -> new String[] {dto.getEISBN(), dto.getPrintISBN(), dto.getDoi(),
                dto.getScopusId(), dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setEISBN(values[0]);
                dto.setPrintISBN(values[1]);
                dto.setDoi(values[2]);
                dto.setScopusId(values[3]);
                dto.setOpenAlexId(values[4]);
                dto.setWebOfScienceId(values[5]);
            });
    }

    @Override
    public void saveMergedPersonsMetadata(Integer leftId, Integer rightId,
                                          PersonalInfoDTO leftData, PersonalInfoDTO rightData) {
        updateAndRestoreMetadata(personService::updatePersonalInfo, personService::indexPerson,
            personService::findOne, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getApvnt(), dto.getECrisId(), dto.getENaukaId(),
                dto.getScopusAuthorId(), dto.getOrcid(), dto.getOpenAlexId(),
                dto.getWebOfScienceResearcherId()},
            (dto, values) -> {
                dto.setApvnt(values[0]);
                dto.setECrisId(values[1]);
                dto.setENaukaId(values[2]);
                dto.setScopusAuthorId(values[3]);
                dto.setOrcid(values[4]);
                dto.setOpenAlexId(values[5]);
                dto.setWebOfScienceResearcherId(values[6]);
            });
    }

    @Override
    public void saveMergedOUsMetadata(Integer leftId, Integer rightId,
                                      OrganisationUnitRequestDTO leftData,
                                      OrganisationUnitRequestDTO rightData) {
        updateAndRestoreMetadata(organisationUnitService::editOrganisationUnit,
            organisationUnitService::indexOrganisationUnit, organisationUnitService::findOne,
            leftId, rightId,
            leftData,
            rightData,
            dto -> new String[] {dto.getScopusAfid(), dto.getOpenAlexId(), dto.getRor()},
            (dto, values) -> {
                dto.setScopusAfid(values[0]);
                dto.setOpenAlexId(values[1]);
                dto.setRor(values[2]);
            });
    }

    @Override
    public void saveMergedJournalsMetadata(Integer leftId, Integer rightId, JournalDTO leftData,
                                           JournalDTO rightData) {
        updateAndRestoreMetadata(journalService::updateJournal, journalService::indexJournal,
            journalService::findJournalById, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getEissn(), dto.getPrintISSN(), dto.getOpenAlexId()},
            (dto, values) -> {
                dto.setEissn(values[0]);
                dto.setPrintISSN(values[1]);
                dto.setOpenAlexId(values[2]);
            });
    }

    @Override
    public void saveMergedBookSeriesMetadata(Integer leftId, Integer rightId,
                                             BookSeriesDTO leftData,
                                             BookSeriesDTO rightData) {
        updateAndRestoreMetadata(bookSeriesService::updateBookSeries,
            bookSeriesService::indexBookSeries, bookSeriesService::findBookSeriesById, leftId,
            rightId, leftData,
            rightData,
            dto -> new String[] {dto.getEissn(), dto.getPrintISSN(), dto.getOpenAlexId()},
            (dto, values) -> {
                dto.setEissn(values[0]);
                dto.setPrintISSN(values[1]);
                dto.setOpenAlexId(values[2]);
            });
    }

    @Override
    public void saveMergedConferencesMetadata(Integer leftId, Integer rightId,
                                              ConferenceDTO leftData, ConferenceDTO rightData) {
        updateAndRestoreMetadata(conferenceService::updateConference,
            conferenceService::indexConference, conferenceService::findConferenceById, leftId,
            rightId, leftData,
            rightData,
            dto -> new String[] {dto.getConfId(), dto.getOpenAlexId()},
            (dto, values) -> {
                dto.setConfId(values[0]);
                dto.setOpenAlexId(values[1]);
            });
    }

    @Override
    public void saveMergedSoftwareMetadata(Integer leftId, Integer rightId, SoftwareDTO leftData,
                                           SoftwareDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(softwareService::editSoftware, softwareService::indexSoftware,
            softwareService::findSoftwareById, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getInternalNumber(), dto.getDoi(), dto.getScopusId(),
                dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setInternalNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
                dto.setOpenAlexId(values[3]);
                dto.setWebOfScienceId(values[4]);
            });
    }

    @Override
    public void saveMergedMaterialProductMetadata(Integer leftId, Integer rightId,
                                                  MaterialProductDTO leftData,
                                                  MaterialProductDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(materialProductService::editMaterialProduct,
            materialProductService::indexMaterialProduct,
            materialProductService::findMaterialProductById, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getInternalNumber(), dto.getDoi(), dto.getScopusId(),
                dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setInternalNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
                dto.setOpenAlexId(values[3]);
                dto.setWebOfScienceId(values[4]);
            });
    }

    @Override
    public void saveMergedGeneticMaterialMetadata(Integer leftId, Integer rightId,
                                                  GeneticMaterialDTO leftData,
                                                  GeneticMaterialDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(geneticMaterialService::editGeneticMaterial,
            geneticMaterialService::indexGeneticMaterial,
            geneticMaterialService::findGeneticMaterialById, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getInternalNumber(), dto.getDoi(), dto.getScopusId(),
                dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setInternalNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
                dto.setOpenAlexId(values[3]);
                dto.setWebOfScienceId(values[4]);
            });
    }

    @Override
    public void saveMergedDatasetsMetadata(Integer leftId, Integer rightId, DatasetDTO leftData,
                                           DatasetDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(datasetService::editDataset, datasetService::indexDataset,
            datasetService::findDatasetById, leftId, rightId, leftData, rightData,
            dto -> new String[] {dto.getInternalNumber(), dto.getDoi(), dto.getScopusId(),
                dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setInternalNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
                dto.setOpenAlexId(values[3]);
                dto.setWebOfScienceId(values[4]);
            });
    }

    @Override
    public void saveMergedPatentsMetadata(Integer leftId, Integer rightId, PatentDTO leftData,
                                          PatentDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(patentService::editPatent, patentService::indexPatent,
            patentService::findPatentById, leftId, rightId, leftData, rightData,
            dto -> new String[] {dto.getNumber(), dto.getDoi(), dto.getScopusId(),
                dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
                dto.setOpenAlexId(values[3]);
                dto.setWebOfScienceId(values[4]);
            });
    }

    @Override
    public void saveMergedProceedingsPublicationMetadata(Integer leftId, Integer rightId,
                                                         ProceedingsPublicationDTO leftData,
                                                         ProceedingsPublicationDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(proceedingsPublicationService::editProceedingsPublication,
            proceedingsPublicationService::indexProceedingsPublication,
            proceedingsPublicationService::findProceedingsPublicationById, leftId,
            rightId, leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId(), dto.getOpenAlexId(),
                dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
                dto.setOpenAlexId(values[2]);
                dto.setWebOfScienceId(values[3]);
            });
    }

    @Override
    public void saveMergedJournalPublicationMetadata(Integer leftId, Integer rightId,
                                                     JournalPublicationDTO leftData,
                                                     JournalPublicationDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(journalPublicationService::editJournalPublication,
            journalPublicationService::indexJournalPublication,
            journalPublicationService::findJournalPublicationById, leftId, rightId,
            leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId(), dto.getOpenAlexId(),
                dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
                dto.setOpenAlexId(values[2]);
                dto.setWebOfScienceId(values[3]);
            });
    }

    @Override
    public void saveMergedThesesMetadata(Integer leftId, Integer rightId, ThesisDTO leftData,
                                         ThesisDTO rightData) {
        updateAndRestoreMetadata(thesisService::editThesis, thesisService::indexThesis,
            thesisService::getThesisById, leftId, rightId, leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId(), dto.getOpenAlexId(),
                dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
                dto.setOpenAlexId(values[2]);
                dto.setWebOfScienceId(values[3]);
            });
    }

    @Override
    public void saveMergedPublishersMetadata(Integer leftId, Integer rightId, PublisherDTO leftData,
                                             PublisherDTO rightData) {
        updateAndRestoreMetadata(publisherService::editPublisher, publisherService::indexPublisher,
            publisherService::findOne, leftId, rightId, leftData,
            rightData, dto -> new String[] {}, (dto, values) -> {
            });
    }

    @Override
    public void switchPublicationToOtherMonograph(Integer targetMonographId,
                                                  Integer publicationId) {
        performMonographPublicationSwitch(targetMonographId, publicationId);
    }

    @Override
    public void switchAllPublicationsToOtherMonograph(Integer sourceMonographId,
                                                      Integer targetMonographId) {
        processChunks(
            sourceMonographId,
            (srcId, monographPublicationIndex) -> performMonographPublicationSwitch(
                targetMonographId, monographPublicationIndex.getDatabaseId()),
            pageRequest -> documentPublicationIndexRepository.findByTypeAndMonographId(
                DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), sourceMonographId,
                pageRequest).getContent()
        );
    }

    @Override
    public void saveMergedMonographsMetadata(Integer leftId, Integer rightId, MonographDTO leftData,
                                             MonographDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(monographService::editMonograph, monographService::indexMonograph,
            monographService::findMonographById, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId(), dto.getPrintISBN(),
                dto.getEisbn(), dto.getOpenAlexId(), dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
                dto.setPrintISBN(values[2]);
                dto.setEisbn(values[3]);
                dto.setOpenAlexId(values[4]);
                dto.setWebOfScienceId(values[5]);
            });
    }

    @Override
    public void saveMergedMonographPublicationsMetadata(Integer leftId, Integer rightId,
                                                        MonographPublicationDTO leftData,
                                                        MonographPublicationDTO rightData) {
        handleNoAuthorsRemaining(leftData, rightData);
        updateAndRestoreMetadata(monographPublicationService::editMonographPublication,
            monographPublicationService::indexMonographPublication,
            monographPublicationService::findMonographPublicationById, leftId,
            rightId,
            leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId(), dto.getOpenAlexId(),
                dto.getWebOfScienceId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
                dto.setOpenAlexId(values[2]);
                dto.setWebOfScienceId(values[3]);
            });
    }

    @Override
    public void migratePersistentIdentifiers(Integer deletionEntityId, Integer mergedEntityId,
                                             EntityType entityType) {
        switch (entityType) {
            case BOOK_SERIES -> migrateIdentifierHistory(bookSeriesService::findRaw,
                bookSeriesService::save, deletionEntityId, mergedEntityId);
            case MONOGRAPH -> migrateIdentifierHistory(monographService::findRaw,
                documentPublicationService::save, deletionEntityId, mergedEntityId);
            case PROCEEDINGS -> migrateIdentifierHistory(proceedingsService::findRaw,
                documentPublicationService::save, deletionEntityId, mergedEntityId);
            case PUBLICATION -> migrateIdentifierHistory(documentPublicationService::findOne,
                documentPublicationService::save, deletionEntityId, mergedEntityId);
            case EVENT -> migrateIdentifierHistory(conferenceService::findRaw,
                conferenceService::save, deletionEntityId, mergedEntityId);
            case JOURNAL -> migrateIdentifierHistory(journalService::findRaw, journalService::save,
                deletionEntityId, mergedEntityId);
            case ORGANISATION_UNIT -> {
                migrateIdentifierHistory(organisationUnitService::findRaw,
                    organisationUnitService::save, deletionEntityId, mergedEntityId);
                migrateAccountingIds(organisationUnitService::findRaw,
                    organisationUnitService::save, deletionEntityId, mergedEntityId);
            }
            case PERSON -> {
                migrateIdentifierHistory(personService::findRaw, personService::save,
                    deletionEntityId, mergedEntityId);
                migrateAccountingIds(personService::findRaw, personService::save,
                    deletionEntityId, mergedEntityId);
            }
            case PUBLISHER ->
                migrateIdentifierHistory(publisherService::findRaw, publisherService::save,
                    deletionEntityId, mergedEntityId);
        }
    }

    private void performMonographPublicationSwitch(Integer targetMonographId,
                                                   Integer monographPublicationId) {
        var targetMonograph = monographService.findMonographById(targetMonographId);
        var monographPublication =
            monographPublicationService.findMonographPublicationById(monographPublicationId);

        monographPublication.setMonograph(targetMonograph);

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
            monographPublicationId, "monograph_id", targetMonographId);
    }

    private void performPersonPublicationSwitch(Integer sourcePersonId, Integer targetPersonId,
                                                Integer publicationId,
                                                boolean skipCoauthoredPublications) {
        var document = documentPublicationService.findDocumentById(publicationId);

        boolean targetPersonFound = false;
        boolean sourcePersonFound = false;

        var newPerson = personService.findOne(targetPersonId);

        for (var contribution : document.getContributors()) {
            if (Objects.isNull(contribution.getPerson())) {
                continue;
            }

            var personId = contribution.getPerson().getId();

            if (personId.equals(targetPersonId)) {
                targetPersonFound = true;
                if (skipCoauthoredPublications) {
                    return;
                }
            }

            if (personId.equals(sourcePersonId)) {
                sourcePersonFound = true;
                contribution.setPerson(newPerson);
            }
        }

        if (targetPersonFound) {
            throw new PersonReferenceConstraintViolationException("alreadyInAuthorListError");
        }

        if (!sourcePersonFound) {
            return; // Source person not found in contributors
        }

        documentRepository.save(document);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            publicationId).orElse(new DocumentPublicationIndex());
        documentPublicationService.indexContributionFields(document, index);
        documentPublicationIndexRepository.save(index);
    }

    private void performJournalPublicationSwitch(Integer targetJournalId, Integer publicationId) {
        var publication = journalPublicationRepository.findById(publicationId);

        if (publication.isEmpty()) {
            throw new NotFoundException("Publication does not exist.");
        }

        var targetJournal = journalService.findJournalById(targetJournalId);

        publication.get().setJournal(targetJournal);

        journalPublicationRepository.save(publication.get());

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
            publicationId, "journal_id", targetJournalId);
    }

    private void performPublisherPublicationSwitch(Integer targetPublisherId,
                                                   Integer publicationId) {
        PublisherPublishable publication = null;

        List<JpaRepository<? extends PublisherPublishable, Integer>> repositories = List.of(
            proceedingsRepository,
            patentRepository,
            datasetRepository,
            softwareRepository,
            thesisRepository,
            monographRepository
        );

        for (var repository : repositories) {
            var result = repository.findById(publicationId);
            if (result.isPresent()) {
                publication = result.get();
                break;
            }
        }

        if (publication == null) {
            throw new NotFoundException("Publication does not exist.");
        }

        var targetPublisher = publisherService.findOne(targetPublisherId);
        publication.setPublisher(targetPublisher);

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
            publicationId, "publisher_id", targetPublisherId);
    }

    private void performBookSeriesPublicationSwitch(Integer targetBookSeriesId,
                                                    Integer publicationId) {
        BookSeriesPublishable publication;
        var proceedings = proceedingsRepository.findById(publicationId);

        if (proceedings.isEmpty()) {
            var monograph = monographRepository.findById(publicationId);
            if (monograph.isEmpty()) {
                throw new NotFoundException("Publication does not exist.");
            } else {
                publication = monograph.get();
            }
        } else {
            publication = proceedings.get();
        }

        var targetBookSeries = bookSeriesService.findBookSeriesById(targetBookSeriesId);

        publication.setPublicationSeries(targetBookSeries);

        if (publication instanceof Proceedings) {
            proceedingsRepository.save((Proceedings) publication);
        } else {
            monographRepository.save((Monograph) publication);
        }

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
            publicationId, "publication_series_id", targetBookSeriesId);
    }

    private void performProceedingsPublicationSwitch(Integer targetProceedingsId,
                                                     Integer publicationId) {
        var publication = proceedingsPublicationRepository.findById(publicationId);

        if (publication.isEmpty()) {
            throw new NotFoundException("Publication does not exist.");
        }

        var targetProceedings = proceedingsService.findProceedingsById(targetProceedingsId);

        publication.get().setProceedings(targetProceedings);

        proceedingsPublicationRepository.save(publication.get());

        indexBulkUpdateService.setIdFieldForRecord("document_publication", "databaseId",
            publicationId, "proceedings_id", targetProceedingsId);
    }

    private void performProceedingsSwitch(Integer targetConferenceId,
                                          Integer proceedingsId) {
        var targetConference = conferenceService.findConferenceById(targetConferenceId);
        var proceedings = proceedingsService.findProceedingsById(proceedingsId);

        if (targetConference.getSerialEvent()) {
            throw new ConferenceReferenceConstraintViolationException(
                "Target conference cannot be serial event.");
        }

        proceedings.setEvent(targetConference);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            proceedingsId).orElse(new DocumentPublicationIndex());
        proceedingsService.indexProceedings(proceedings, index);

        proceedingsPublicationRepository.switchAllFromProceedingsToNewEvent(proceedingsId,
            targetConference);
        indexBulkUpdateService.setIdFieldForRecord("document_publication", "proceedings_id",
            proceedingsId, "event_id", targetConferenceId);
    }

    private void performEmployeeSwitch(Integer sourceOUId, Integer targetOUId, Integer personId,
                                       boolean reindexPersonEmploymentInformation) {
        var person = personService.findOne(personId);

        person.getInvolvements().forEach(involvement -> {
            if ((involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                Objects.nonNull(involvement.getOrganisationUnit()) &&
                involvement.getOrganisationUnit().getId().equals(sourceOUId)) {
                involvement.setOrganisationUnit(organisationUnitService.findOne(targetOUId));
            }
        });

        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);

        if (reindexPersonEmploymentInformation) {
            personService.reindexPersonEmploymentDetails(person);
            applicationEventPublisher.publishEvent(
                new PersonEmploymentOUHierarchyStructureChangedEvent(
                    person.getId()));
        }
    }

    private <T> void processChunks(int sourceId,
                                   BiConsumer<Integer, T> switchOperation,
                                   Function<PageRequest, List<T>> fetchChunk) {
        var pageNumber = 0;
        var chunkSize = 100;
        var hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk = fetchChunk.apply(PageRequest.of(pageNumber, chunkSize));

            chunk.forEach(item -> switchOperation.accept(sourceId, item));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private void mergeDocumentFiles(Document leftDocument, Document rightDocument,
                                    List<Integer> leftFileIds, List<Integer> rightFileIds) {
        mergeFiles(leftDocument, rightDocument, leftFileIds);
        mergeFiles(rightDocument, leftDocument, rightFileIds);
    }

    private void mergeFiles(Document sourceDocument, Document targetDocument,
                            List<Integer> sourceFileIds) {
        sourceFileIds.forEach(fileId -> {
            if (sourceDocument.getProofs().stream()
                .noneMatch(file -> file.getId().equals(fileId))) {
                var fileForMerging = targetDocument.getProofs().stream()
                    .filter(file -> file.getId().equals(fileId)).findFirst();

                if (fileForMerging.isEmpty()) {
                    throw new NotFoundException(
                        "Non-existing document file specified for merging.");
                }

                targetDocument.getProofs().remove(fileForMerging.get());
                sourceDocument.getProofs().add(fileForMerging.get());
            }
        });
    }

    private <T, R extends Mergeable> void updateAndRestoreMetadata(
        BiConsumer<Integer, T> updateMethod,
        Consumer<R> reindexMethod,
        Function<Integer, R> fetchFunction,
        Integer leftId, Integer rightId,
        T leftData, T rightData,
        Function<T, String[]> originalValuesExtractor,
        BiConsumer<T, String[]> restoreValues) {
        String[] originalValues = originalValuesExtractor.apply(leftData);

        String[] emptyValues = new String[originalValues.length];
        Arrays.fill(emptyValues, "");
        restoreValues.accept(leftData, emptyValues);

        updateMethod.accept(leftId, leftData);
        updateMethod.accept(rightId, rightData);

        restoreValues.accept(leftData, originalValues);
        updateMethod.accept(leftId, leftData);

        var leftEntity = fetchFunction.apply(leftId);
        var rightEntity = fetchFunction.apply(rightId);

        reindexMethod.accept(leftEntity);
        reindexMethod.accept(rightEntity);
    }

    private <T extends Mergeable> void migrateIdentifierHistory(Function<Integer, T> fetchFunction,
                                                                Consumer<T> saveMethod,
                                                                Integer deletionEntityId,
                                                                Integer mergedEntityId) {
        var deletionEntity = fetchFunction.apply(deletionEntityId);
        var mergedEntity = fetchFunction.apply(mergedEntityId);

        mergedEntity.getMergedIds().addAll(deletionEntity.getMergedIds());
        mergedEntity.getMergedIds().add(deletionEntityId);
        mergedEntity.getOldIds().addAll(deletionEntity.getOldIds());

        saveMethod.accept(mergedEntity);
    }

    private <T extends Accounted> void migrateAccountingIds(Function<Integer, T> fetchFunction,
                                                            Consumer<T> saveMethod,
                                                            Integer deletionEntityId,
                                                            Integer mergedEntityId) {

        var deletionEntity = fetchFunction.apply(deletionEntityId);
        var mergedEntity = fetchFunction.apply(mergedEntityId);

        mergedEntity.getAccountingIds().addAll(deletionEntity.getAccountingIds());

        saveMethod.accept(mergedEntity);
    }

    private void handleNoAuthorsRemaining(DocumentDTO leftData, DocumentDTO rightData) {
        var loggedInUser =
            (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (loggedInUser.getAuthority().getAuthority().equals(UserRole.ADMIN.name())) {
            return;
        }

        if (leftData.getContributions().isEmpty()) {
            var byPerson = rightData.getContributions().stream()
                .filter(c -> Objects.nonNull(c.getPersonId()))
                .collect(Collectors.groupingBy(PersonContributionDTO::getPersonId));

            byPerson.values().stream()
                .filter(list -> list.size() > 1)
                .map(list -> list.get(1)) // second occurrence
                .forEach(contribution -> leftData.getContributions().add(contribution));

        } else if (rightData.getContributions().isEmpty()) {
            var byPerson = leftData.getContributions().stream()
                .filter(c -> Objects.nonNull(c.getPersonId()))
                .collect(Collectors.groupingBy(PersonContributionDTO::getPersonId));

            byPerson.values().stream()
                .filter(list -> list.size() > 1)
                .map(list -> list.get(1)) // second occurrence
                .forEach(contribution -> rightData.getContributions().add(contribution));
        }
    }
}
