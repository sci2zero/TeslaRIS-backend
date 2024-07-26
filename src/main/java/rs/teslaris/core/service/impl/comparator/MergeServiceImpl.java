package rs.teslaris.core.service.impl.comparator;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
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
}
