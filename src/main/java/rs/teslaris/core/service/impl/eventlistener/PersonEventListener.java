package rs.teslaris.core.service.impl.eventlistener;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.core.applicationevent.OrganisationUnitDeletedEvent;
import rs.teslaris.core.applicationevent.OrganisationUnitSignificantChangeEvent;
import rs.teslaris.core.applicationevent.PersonContributionsChangeEvent;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.CollectionOperations;

@Component
@RequiredArgsConstructor
public class PersonEventListener {

    private final PersonService personService;

    private final PersonIndexRepository personIndexRepository;

    private final PersonRepository personRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final PrizeService prizeService;


    @Async("taskExecutor")
    @EventListener
    protected void handleOUSignificantChange(OrganisationUnitSignificantChangeEvent event) {
        reindexInstitutionEmployeesEmployments(event.getOrganisationUnitId());
    }

    @Async("taskExecutor")
    @EventListener
    protected void handleOUDeletion(OrganisationUnitDeletedEvent event) {
        reindexInstitutionEmployeesEmployments(event.getOrganisationUnitId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    protected void handleAddedContribution(PersonContributionsChangeEvent event) {
        if (Objects.isNull(event) || !CollectionOperations.containsValues(event.getPersonIds())) {
            return;
        }

        FunctionalUtil.performBulkOperation(
            pageable -> personIndexRepository.findByDatabaseIdIn(
                event.getPersonIds().stream().toList(), pageable),
            Sort.by(Sort.Direction.ASC, "databaseId"),
            (personIndex) -> {
                personIndex.setHasContributions(
                    personRepository.hasContribution(personIndex.getDatabaseId()));

                personIndexRepository.save(personIndex);
            }
        );
    }

    private void reindexInstitutionEmployeesEmployments(Integer organisationUnitId) {
        int pageNumber = 0;
        int chunkSize = 500;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<PersonIndex> chunk = personIndexRepository.findByInstitutionId(organisationUnitId,
                    PageRequest.of(pageNumber, chunkSize, Sort.by(Sort.Direction.ASC, "databaseId")))
                .getContent();

            chunk.forEach(
                index -> {
                    var person =
                        personRepository.findOneWithInvolvementsAndPrizes(index.getDatabaseId())
                            .orElse(null);

                    personService.setPersonIndexEmploymentDetails(index, person);
                    personIndexRepository.save(index);

                    person.getEmploymentInstitutionsIdHierarchy().addAll(
                        index.getEmploymentInstitutionsIdHierarchy());
                    personRepository.save(person);

                    if (Objects.nonNull(person)) {
                        person.getPrizes().forEach(prize ->
                            prizeService.reindexPrizeVolatileInformation(prize, null,
                                true, false));
                    }

                    applicationEventPublisher.publishEvent(
                        new PersonEmploymentOUHierarchyStructureChangedEvent(
                            index.getDatabaseId()));
                });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
