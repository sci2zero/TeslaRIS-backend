package rs.teslaris.core.service.impl.person.worker;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.ExtraEmploymentMigrationDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.EmploymentRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.SearchRequestType;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmploymentMigrationWorker {

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final EmploymentRepository employmentRepository;

    private final InvolvementRepository involvementRepository;

    private final UserService userService;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class,
            org.springframework.dao.DeadlockLoserDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 2))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleMigration(ExtraEmploymentMigrationDTO migration,
                                       HashSet<Integer> handledInstitutionIds,
                                       HashSet<Integer> handledPersonIds) {
        var ouResults = organisationUnitService.searchOrganisationUnits(
            new ArrayList<>(
                Arrays.stream(migration.organisationUnitName().split(" ")).toList()),
            PageRequest.of(0, 1),
            SearchRequestType.SIMPLE,
            null, null, null,
            null, null, null,
            null, null).getContent();

        if (ouResults.isEmpty()) {
            log.warn("Unable to find OU with name {} when migrating employments.",
                migration.organisationUnitName());
            return;
        }

        var person = personService.findPersonByAccountingId(
            String.valueOf(migration.personAccountingId()));
        if (Objects.isNull(person)) {
            var newPerson = new BasicPersonDTO();
            newPerson.setPersonName(migration.personName());
            newPerson.setOrganisationUnitId(ouResults.getFirst().getDatabaseId());
            newPerson.setEmploymentPosition(migration.employmentPosition());

            person = personService.createPersonWithBasicInfo(newPerson, true);
            person.getAccountingIds().add(String.valueOf(migration.personAccountingId()));
            personService.save(person);
        }

        var existingEmployment =
            findExistingEmployment(person, ouResults.getFirst().getDatabaseId(),
                migration.employmentPosition());

        Employment employment;
        if (existingEmployment.isPresent()) {
            updateExistingEmployment((Employment) existingEmployment.get(),
                migration.employmentStartDate());
        } else {
            employment =
                createNewMigratedEmployment(person,
                    organisationUnitService.findOne(ouResults.getFirst().getDatabaseId()),
                    migration.employmentStartDate(), migration.employmentPosition());
            closeOtherOpenEmployments(person, ouResults.getFirst().getDatabaseId(),
                employment);
        }

        var savedPerson = personService.save(person);
        personService.indexPerson(savedPerson);
        userService.updateResearcherCurrentOrganisationUnitIfBound(savedPerson.getId());

        handledPersonIds.add(savedPerson.getId());
        handledInstitutionIds.add(ouResults.getFirst().getDatabaseId());
    }

    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class,
            org.springframework.dao.DeadlockLoserDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 2))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAlumni(Employment employment,
                              HashSet<Integer> handledPersonIds,
                              ArrayList<Integer> employmentIdsForDeletion,
                              ArrayList<Integer> personIdsForDeletion,
                              LocalDate initialMigrationDate) {
        var personInvolved = personService.findPersonById(employment.getPersonInvolved().getId());

        if (!handledPersonIds.contains(personInvolved.getId())) {
            if (!personService.personHasContributions(personInvolved.getId()) &&
                personInvolved.getCreateDate().equals(
                    Date.from(
                        initialMigrationDate.atStartOfDay(ZoneId.systemDefault())
                            .toInstant()))) {
                employmentIdsForDeletion.add(employment.getId());
                personIdsForDeletion.add(personInvolved.getId());
                return;
            }

            if (Objects.isNull(employment.getDateTo())) {
                employment.setDateTo(LocalDate.of(2023, 12, 31));
            }

            employmentRepository.save(employment);
            personService.save(personInvolved);
            personService.indexPerson(personInvolved);
        }
    }

    @Retryable(
        value = {org.springframework.dao.CannotAcquireLockException.class,
            org.springframework.dao.DeadlockLoserDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 2))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Employment performLegacyMigration(EmploymentMigrationDTO employmentMigrationRequest) {
        Person person = resolvePerson(employmentMigrationRequest);
        OrganisationUnit organisationUnit = resolveOrganisationUnit(employmentMigrationRequest);

        var existingEmployment = findExistingEmployment(person, organisationUnit.getId(),
            employmentMigrationRequest.employmentPosition());

        Employment employment;
        if (existingEmployment.isPresent()) {
            employment = updateExistingEmployment((Employment) existingEmployment.get(),
                employmentMigrationRequest.employmentStartDate());
        } else {
            employment =
                createNewMigratedEmployment(person, organisationUnit,
                    employmentMigrationRequest.employmentStartDate(),
                    employmentMigrationRequest.employmentPosition());
            closeOtherOpenEmployments(person, organisationUnit.getId(), employment);
        }

        personService.save(person);
        userService.updateResearcherCurrentOrganisationUnitIfBound(person.getId());

        // TODO: This is an administrative task that we run once a year, maybe a better solution
        //  for this is to run reindexing task after migration, to avoid so many index updates
        //  with each new insert.
        applicationEventPublisher.publishEvent(
            new PersonEmploymentOUHierarchyStructureChangedEvent(person.getId()));

        return employment;
    }

    private Optional<Involvement> findExistingEmployment(Person person, Integer organisationUnitId,
                                                         EmploymentPosition position) {
        return person.getInvolvements().stream()
            .filter(involvement ->
                involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) &&
                    involvement.getOrganisationUnit().getId().equals(organisationUnitId) &&
                    ((Employment) involvement).getEmploymentPosition().equals(position))
            .findAny();
    }

    private Employment updateExistingEmployment(Employment employment,
                                                LocalDate employmentStartDate) {
        employment.setDateFrom(employmentStartDate);
        employment.setDateTo(null);
        return involvementRepository.save(employment);
    }

    private Employment createNewMigratedEmployment(Person person, OrganisationUnit organisationUnit,
                                                   LocalDate employmentStartDate,
                                                   EmploymentPosition position) {
        var employment = new Employment();
        employment.setDateFrom(employmentStartDate);
        employment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        employment.setOrganisationUnit(organisationUnit);
        employment.setEmploymentPosition(position);
        employment.setApproveStatus(ApproveStatus.APPROVED);

        person.addInvolvement(employment);
        Employment saved = employmentRepository.save(employment);
        personService.indexPerson(person);
        return saved;
    }

    private void closeOtherOpenEmployments(Person person, Integer organisationUnitId,
                                           Employment newEmployment) {
        person.getInvolvements().stream()
            .filter(involvement ->
                involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) &&
                    involvement.getOrganisationUnit().getId().equals(organisationUnitId) &&
                    involvement != newEmployment &&
                    (Objects.isNull(involvement.getDateTo()) ||
                        involvement.getDateTo().isAfter(LocalDate.now())))
            .forEach(involvement -> involvement.setDateTo(newEmployment.getDateFrom()));
    }

    private Person resolvePerson(EmploymentMigrationDTO request) {
        var person = Objects.nonNull(request.personOldId())
            ? personService.findPersonByOldId(request.personOldId())
            : personService.findPersonByAccountingId(request.personAccountingId());

        if (Objects.isNull(person)) {
            throw new NotFoundException(
                "Unable to find person (oldId: " + request.personOldId() + " | accountingId: " +
                    request.personAccountingId() + ") when migrating employment.");
        }

        person.getAccountingIds().add(request.personAccountingId());
        return person;
    }

    private OrganisationUnit resolveOrganisationUnit(EmploymentMigrationDTO request) {
        var unit = Objects.nonNull(request.chairOldId())
            ? organisationUnitService.findOrganisationUnitByOldId(request.chairOldId())
            :
            organisationUnitService.findOrganisationUnitByAccountingId(request.chairAccountingId());

        if (Objects.isNull(unit)) {
            throw new NotFoundException(
                "Unable to find OU (oldId: " + request.chairOldId() + " | accountingId: " +
                    request.chairAccountingId() + ") when migrating employment.");
        }

        unit.getAccountingIds().add(request.chairAccountingId());
        organisationUnitService.save(unit);
        return unit;
    }
}
