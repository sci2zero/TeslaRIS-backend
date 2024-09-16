package rs.teslaris.core.service.impl.comparator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.BookSeriesPublishable;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.merge.MergeService;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Transactional
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

    private final DatasetService datasetService;

    private final PatentService patentService;

    private final ThesisService thesisService;

    private final MonographService monographService;

    private final MonographPublicationService monographPublicationService;

    private final ProceedingsRepository proceedingsRepository;

    private final MonographRepository monographRepository;

    private final BookSeriesService bookSeriesService;


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
    public void switchAllPublicationToOtherPerson(Integer sourcePersonId, Integer targetPersonId) {
        processChunks(
            sourcePersonId,
            (srcId, personPublicationIndex) -> performPersonPublicationSwitch(srcId, targetPersonId,
                personPublicationIndex.getDatabaseId(), true),
            pageRequest -> documentPublicationService.findResearcherPublications(sourcePersonId,
                pageRequest).getContent()
        );
    }

    @Override
    public void switchPersonToOtherOU(Integer sourceOUId, Integer targetOUId, Integer personId) {
        performEmployeeSwitch(sourceOUId, targetOUId, personId);
    }

    @Override
    public void switchAllPersonsToOtherOU(Integer sourceOUId, Integer targetOUId) {
        processChunks(
            sourceOUId,
            (srcId, personIndex) -> performEmployeeSwitch(srcId, targetOUId,
                personIndex.getDatabaseId()),
            pageRequest -> personService.findPeopleForOrganisationUnit(sourceOUId, pageRequest)
                .getContent()
        );
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
                                   Integer targetPersonId) {
        var sourcePerson = personService.findOne(sourcePersonId);
        var targetPerson = personService.findOne(targetPersonId);

        involvementIds.forEach(involvementId -> {
            var involvementToUpdate = involvementService.findOne(involvementId);

            if (sourcePerson.getInvolvements().contains(involvementToUpdate)) {
                sourcePerson.removeInvolvement(involvementToUpdate);
                involvementService.save(involvementToUpdate);
            }

            if (!targetPerson.getInvolvements().contains(involvementToUpdate)) {
                involvementToUpdate.setPersonInvolved(targetPerson);
                targetPerson.addInvolvement(involvementToUpdate);
                involvementService.save(involvementToUpdate);
            }
        });

        personService.save(sourcePerson);
        personService.save(targetPerson);

        userService.updateResearcherCurrentOrganisationUnitIfBound(sourcePersonId);
        userService.updateResearcherCurrentOrganisationUnitIfBound(targetPersonId);

        personService.indexPerson(sourcePerson, sourcePerson.getId());
        personService.indexPerson(targetPerson, targetPerson.getId());
    }

    @Override
    public void switchSkills(List<Integer> skillIds, Integer sourcePersonId,
                             Integer targetPersonId) {
        var sourcePerson = personService.findOne(sourcePersonId);
        var targetPerson = personService.findOne(targetPersonId);

        skillIds.forEach(skillId -> {
            var skillToUpdate = expertiseOrSkillService.findOne(skillId);

            sourcePerson.getExpertisesAndSkills().remove(skillToUpdate);

            targetPerson.getExpertisesAndSkills().add(skillToUpdate);
        });

        personService.save(sourcePerson);
        personService.save(targetPerson);
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
        });

        personService.save(sourcePerson);
        personService.save(targetPerson);
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
        updateAndRestoreMetadata(proceedingsService::updateProceedings, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getEISBN(), dto.getPrintISBN(), dto.getDoi(),
                dto.getScopusId()},
            (dto, values) -> {
                dto.setEISBN(values[0]);
                dto.setPrintISBN(values[1]);
                dto.setDoi(values[2]);
                dto.setScopusId(values[3]);
            });
    }

    @Override
    public void saveMergedPersonsMetadata(Integer leftId, Integer rightId,
                                          PersonalInfoDTO leftData, PersonalInfoDTO rightData) {
        updateAndRestoreMetadata(personService::updatePersonalInfo, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getApvnt(), dto.getECrisId(), dto.getENaukaId(),
                dto.getScopusAuthorId(), dto.getOrcid()},
            (dto, values) -> {
                dto.setApvnt(values[0]);
                dto.setECrisId(values[1]);
                dto.setENaukaId(values[2]);
                dto.setScopusAuthorId(values[3]);
                dto.setOrcid(values[4]);
            });
    }

    @Override
    public void saveMergedJournalsMetadata(Integer leftId, Integer rightId, JournalDTO leftData,
                                           JournalDTO rightData) {
        updateAndRestoreMetadata(journalService::updateJournal, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getEissn(), dto.getPrintISSN()},
            (dto, values) -> {
                dto.setEissn(values[0]);
                dto.setPrintISSN(values[1]);
            });
    }

    @Override
    public void saveMergedBookSeriesMetadata(Integer leftId, Integer rightId,
                                             BookSeriesDTO leftData,
                                             BookSeriesDTO rightData) {
        updateAndRestoreMetadata(bookSeriesService::updateBookSeries, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getEissn(), dto.getPrintISSN()},
            (dto, values) -> {
                dto.setEissn(values[0]);
                dto.setPrintISSN(values[1]);
            });
    }

    @Override
    public void saveMergedConferencesMetadata(Integer leftId, Integer rightId,
                                              ConferenceDTO leftData, ConferenceDTO rightData) {
        updateAndRestoreMetadata(conferenceService::updateConference, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getConfId()},
            (dto, values) -> dto.setConfId(values[0]));
    }

    @Override
    public void saveMergedSoftwareMetadata(Integer leftId, Integer rightId, SoftwareDTO leftData,
                                           SoftwareDTO rightData) {
        updateAndRestoreMetadata(softwareService::editSoftware, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getInternalNumber(), dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setInternalNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
            });
    }

    @Override
    public void saveMergedDatasetsMetadata(Integer leftId, Integer rightId, DatasetDTO leftData,
                                           DatasetDTO rightData) {
        updateAndRestoreMetadata(datasetService::editDataset, leftId, rightId, leftData, rightData,
            dto -> new String[] {dto.getInternalNumber(), dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setInternalNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
            });
    }

    @Override
    public void saveMergedPatentsMetadata(Integer leftId, Integer rightId, PatentDTO leftData,
                                          PatentDTO rightData) {
        updateAndRestoreMetadata(patentService::editPatent, leftId, rightId, leftData, rightData,
            dto -> new String[] {dto.getNumber(), dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setNumber(values[0]);
                dto.setDoi(values[1]);
                dto.setScopusId(values[2]);
            });
    }

    @Override
    public void saveMergedProceedingsPublicationMetadata(Integer leftId, Integer rightId,
                                                         ProceedingsPublicationDTO leftData,
                                                         ProceedingsPublicationDTO rightData) {
        updateAndRestoreMetadata(proceedingsPublicationService::editProceedingsPublication, leftId,
            rightId, leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
            });
    }

    @Override
    public void saveMergedJournalPublicationMetadata(Integer leftId, Integer rightId,
                                                     JournalPublicationDTO leftData,
                                                     JournalPublicationDTO rightData) {
        updateAndRestoreMetadata(journalPublicationService::editJournalPublication, leftId, rightId,
            leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
            });
    }

    @Override
    public void saveMergedThesesMetadata(Integer leftId, Integer rightId, ThesisDTO leftData,
                                         ThesisDTO rightData) {
        updateAndRestoreMetadata(thesisService::editThesis, leftId, rightId, leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
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
        updateAndRestoreMetadata(monographService::editMonograph, leftId, rightId, leftData,
            rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId(), dto.getPrintISBN(),
                dto.getEisbn()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
                dto.setPrintISBN(values[2]);
                dto.setEisbn(values[3]);
            });
    }

    @Override
    public void saveMergedMonographPublicationsMetadata(Integer leftId, Integer rightId,
                                                        MonographPublicationDTO leftData,
                                                        MonographPublicationDTO rightData) {
        updateAndRestoreMetadata(monographPublicationService::editMonographPublication, leftId,
            rightId,
            leftData, rightData,
            dto -> new String[] {dto.getDoi(), dto.getScopusId()},
            (dto, values) -> {
                dto.setDoi(values[0]);
                dto.setScopusId(values[1]);
            });
    }

    private void performMonographPublicationSwitch(Integer targetMonographId,
                                                   Integer monographPublicationId) {
        var targetMonograph = monographService.findMonographById(targetMonographId);
        var monographPublication =
            monographPublicationService.findMonographPublicationById(monographPublicationId);

        monographPublication.setMonograph(targetMonograph);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            monographPublicationId).orElse(new DocumentPublicationIndex());
        monographPublicationService.indexMonographPublication(monographPublication, index);
    }

    private void performPersonPublicationSwitch(Integer sourcePersonId, Integer targetPersonId,
                                                Integer publicationId,
                                                boolean skipCoauthoredPublications) {
        var document = documentPublicationService.findDocumentById(publicationId);

        for (var contribution : document.getContributors()) {
            if (Objects.nonNull(contribution.getPerson()) &&
                contribution.getPerson().getId().equals(targetPersonId)) {
                if (skipCoauthoredPublications) {
                    return;
                } else {
                    throw new PersonReferenceConstraintViolationException(
                        "allreadyInAuthorListError");
                }
            }
        }

        document.getContributors().forEach(contribution -> {
            if (Objects.nonNull(contribution.getPerson()) &&
                contribution.getPerson().getId().equals(sourcePersonId)) {
                var newPerson = personService.findOne(targetPersonId);
                contribution.setPerson(newPerson);

                if (!newPerson.getName()
                    .equals(contribution.getAffiliationStatement().getDisplayPersonName()) &&
                    !newPerson.getOtherNames()
                        .contains(contribution.getAffiliationStatement().getDisplayPersonName())) {
                    contribution.getAffiliationStatement().getDisplayPersonName()
                        .setFirstname(newPerson.getName().getFirstname());
                    contribution.getAffiliationStatement().getDisplayPersonName()
                        .setOtherName("");
                    contribution.getAffiliationStatement().getDisplayPersonName()
                        .setLastname(newPerson.getName().getLastname());
                }

            }
        });

        documentRepository.save(document);

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            publicationId).orElse(new DocumentPublicationIndex());
        documentPublicationService.indexCommonFields(document, index);
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

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            publicationId).orElse(new DocumentPublicationIndex());
        journalPublicationService.indexJournalPublication(publication.get(), index);
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

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            publicationId).orElse(new DocumentPublicationIndex());
        if (publication instanceof Proceedings) {
            proceedingsRepository.save((Proceedings) publication);
            proceedingsService.indexProceedings((Proceedings) publication, index);
        } else {
            monographRepository.save((Monograph) publication);
            monographService.indexMonograph((Monograph) publication, index);
        }
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

        var index = documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            publicationId).orElse(new DocumentPublicationIndex());
        proceedingsPublicationService.indexProceedingsPublication(publication.get(), index);
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
    }

    private void performEmployeeSwitch(Integer sourceOUId, Integer targetOUId, Integer personId) {
        var person = personService.findOne(personId);

        person.getInvolvements().forEach(involvement -> {
            if ((involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                involvement.getOrganisationUnit().getId().equals(sourceOUId)) {
                involvement.setOrganisationUnit(organisationUnitService.findOne(targetOUId));
            }
        });

        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);
        personService.indexPerson(person, personId);
    }

    private <T> void processChunks(int sourceId,
                                   BiConsumer<Integer, T> switchOperation,
                                   Function<PageRequest, List<T>> fetchChunk) {
        var pageNumber = 0;
        var chunkSize = 10;
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

    private <T> void updateAndRestoreMetadata(BiConsumer<Integer, T> updateMethod,
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
    }
}
