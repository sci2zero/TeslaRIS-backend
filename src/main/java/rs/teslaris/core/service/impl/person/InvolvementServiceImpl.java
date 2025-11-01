package rs.teslaris.core.service.impl.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.person.InvolvementConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.InternalIdentifierMigrationDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.ExtraEmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.person.EmploymentRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.SearchRequestType;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@Traceable
public class InvolvementServiceImpl extends JPAServiceImpl<Involvement>
    implements InvolvementService {

    private final PersonService personService;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final InvolvementRepository involvementRepository;

    private final DocumentFileService documentFileService;

    private final EmploymentRepository employmentRepository;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    protected JpaRepository<Involvement, Integer> getEntityRepository() {
        return involvementRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Involvement findInvolvementById(Integer involvementId) {
        return involvementRepository.findById(involvementId)
            .orElseThrow(() -> new NotFoundException("Involvement with given ID does not exist."));
    }

    @Override
    public <T extends Involvement, R> R getInvolvement(Integer involvementId,
                                                       Class<T> involvementClass) {
        var involvement = findOne(involvementId);
        return (R) InvolvementConverter.toDTO(involvementClass.cast(involvement));
    }

    @Override
    public Education addEducation(Integer personId, EducationDTO education) {
        var personInvolved = personService.findOne(personId);

        var thesisTitle =
            multilingualContentService.getMultilingualContent(education.getThesisTitle());
        var title = multilingualContentService.getMultilingualContent(education.getTitle());
        var abbreviationTitle =
            multilingualContentService.getMultilingualContent(education.getAbbreviationTitle());

        var newEducation = new Education();
        setCommonFields(newEducation, education);
        newEducation.setThesisTitle(thesisTitle);
        newEducation.setTitle(title);
        newEducation.setAbbreviationTitle(abbreviationTitle);

        personInvolved.addInvolvement(newEducation);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);

        return involvementRepository.save(newEducation);
    }

    @Override
    public Membership addMembership(Integer personId, MembershipDTO membership) {
        var personInvolved = personService.findOne(personId);

        var contributorDescription = multilingualContentService.getMultilingualContent(
            membership.getContributionDescription());
        var role = multilingualContentService.getMultilingualContent(membership.getRole());

        var newMembership = new Membership();
        setCommonFields(newMembership, membership);
        newMembership.setContributionDescription(contributorDescription);
        newMembership.setRole(role);

        personInvolved.addInvolvement(newMembership);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);

        return involvementRepository.save(newMembership);
    }

    @Override
    public List<EmploymentDTO> getDirectAndIndirectEmploymentsForPerson(Integer personId) {
        var directAndIndirectEmployments = new ArrayList<EmploymentDTO>();

        employmentRepository.findByPersonInvolvedId(personId).forEach(employment -> {
            if (Objects.isNull(employment.getOrganisationUnit())) {
                return;
            }

            if (directAndIndirectEmployments.stream()
                .anyMatch(e -> e.getOrganisationUnitId()
                    .equals(employment.getOrganisationUnit().getId()))) {
                return;
            }

            directAndIndirectEmployments.add(InvolvementConverter.toDTO(employment));

            var employmentOU = employment.getOrganisationUnit();
            organisationUnitService.getSuperOUsHierarchyRecursive(employmentOU.getId())
                .forEach(indirectSuperEmployment -> {
                    if (directAndIndirectEmployments.stream()
                        .anyMatch(e -> e.getOrganisationUnitId().equals(indirectSuperEmployment))) {
                        return;
                    }

                    directAndIndirectEmployments.add(new EmploymentDTO() {{
                        setOrganisationUnitId(indirectSuperEmployment);
                        setOrganisationUnitName(
                            MultilingualContentConverter.getMultilingualContentDTO(
                                organisationUnitService.findOne(indirectSuperEmployment)
                                    .getName()));
                    }});
                });
        });

        return directAndIndirectEmployments;
    }

    @Override
    public List<Integer> getDirectEmploymentInstitutionIdsForPerson(Integer personId) {
        var employmentInstitutionIds = new ArrayList<Integer>();

        employmentRepository.findByPersonInvolvedId(personId).forEach(employment -> {
            if (Objects.isNull(employment.getOrganisationUnit())) {
                return;
            }

            employmentInstitutionIds.add(employment.getOrganisationUnit().getId());
        });

        return employmentInstitutionIds;
    }

    @Override
    public Employment addEmployment(Integer personId, EmploymentDTO employment) {
        var personInvolved = personService.findOne(personId);

        var role = multilingualContentService.getMultilingualContent(employment.getRole());

        var newEmployment = new Employment();
        setCommonFields(newEmployment, employment);
        newEmployment.setEmploymentPosition(employment.getEmploymentPosition());
        newEmployment.setRole(role);

        personInvolved.addInvolvement(newEmployment);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);
        personService.indexPerson(personInvolved);

        var savedEmployment = involvementRepository.save(newEmployment);

        applicationEventPublisher.publishEvent(
            new PersonEmploymentOUHierarchyStructureChangedEvent(personId));

        return savedEmployment;
    }

    @Override
    public EmploymentDTO migrateEmployment(EmploymentMigrationDTO employmentMigrationRequest) {
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

        return InvolvementConverter.toDTO(employment);
    }

    @Override
    public void migrateEmployment(List<ExtraEmploymentMigrationDTO> request) {
        request.forEach(migration -> {
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

            personService.save(person);
            userService.updateResearcherCurrentOrganisationUnitIfBound(person.getId());
        });

        // TODO: Update employment hierarchy for all users
    }

    @Override
    public void migrateEmployeeInternalIdentifiers(InternalIdentifierMigrationDTO dto) {
        var institutionIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(dto.institutionId());
        involvementRepository.findActiveEmploymentsForInstitutions(institutionIds)
            .forEach(employment -> {
                if (Objects.isNull(employment.getPersonInvolved())) {
                    return;
                }

                var migrated = false;
                for (var oldId : employment.getPersonInvolved().getOldIds()) {
                    if (dto.oldToInternalIdMapping().containsKey(oldId)) {
                        if (dto.accountingIds()) {
                            employment.getPersonInvolved().getAccountingIds()
                                .add(String.valueOf(dto.oldToInternalIdMapping().get(oldId)));
                        } else {
                            employment.getPersonInvolved().getInternalIdentifiers()
                                .add(String.valueOf(dto.oldToInternalIdMapping().get(oldId)));
                        }
                        migrated = true;
                        personService.save(employment.getPersonInvolved());
                    }
                }

                if (!migrated) {
                    employment.setDateTo(dto.defaultInvolvementEndDate());
                }

                save(employment);
            });
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
                    Objects.isNull(involvement.getDateTo()))
            .forEach(involvement -> involvement.setDateTo(newEmployment.getDateFrom()));
    }

    @Override
    public DocumentFileResponseDTO addInvolvementProof(DocumentFileDTO proof,
                                                       Integer involvementId) {
        var involvement = findOne(involvementId);
        var documentFile = documentFileService.saveNewPersonalDocument(proof, false,
            involvement.getPersonInvolved());
        involvement.getProofs().add(documentFile);
        involvementRepository.save(involvement);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public void deleteProof(Integer proofId, Integer involvementId) {
        var involvement = findOne(involvementId);
        var documentFile = documentFileService.findDocumentFileById(proofId);

        involvement.setProofs(involvement.getProofs().stream()
            .filter(proof -> !Objects.equals(proof.getId(), proofId)).collect(
                Collectors.toSet()));
        save(involvement);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    @Override
    public DocumentFileResponseDTO updateProof(Integer proofId, Integer involvementId,
                                               DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    public void updateEducation(Integer involvementId, EducationDTO education) {
        var educationToUpdate = (Education) findOne(involvementId);

        var thesisTitle =
            multilingualContentService.getMultilingualContent(education.getThesisTitle());
        var title = multilingualContentService.getMultilingualContent(education.getTitle());
        var abbreviationTitle =
            multilingualContentService.getMultilingualContent(education.getAbbreviationTitle());

        clearCommonCollections(educationToUpdate);
        educationToUpdate.getThesisTitle().clear();
        educationToUpdate.getTitle().clear();
        educationToUpdate.getAbbreviationTitle().clear();

        setCommonFields(educationToUpdate, education);
        educationToUpdate.setThesisTitle(thesisTitle);
        educationToUpdate.setTitle(title);
        educationToUpdate.setAbbreviationTitle(abbreviationTitle);

        involvementRepository.save(educationToUpdate);
        userService.updateResearcherCurrentOrganisationUnitIfBound(
            educationToUpdate.getPersonInvolved().getId());
    }

    @Override
    public void updateMembership(Integer involvementId, MembershipDTO membership) {
        var membershipToUpdate = (Membership) findOne(involvementId);

        var contributorDescription = multilingualContentService.getMultilingualContent(
            membership.getContributionDescription());
        var role = multilingualContentService.getMultilingualContent(membership.getRole());

        clearCommonCollections(membershipToUpdate);
        membershipToUpdate.getContributionDescription().clear();
        membershipToUpdate.getRole().clear();

        setCommonFields(membershipToUpdate, membership);
        membershipToUpdate.setContributionDescription(contributorDescription);
        membershipToUpdate.setRole(role);

        involvementRepository.save(membershipToUpdate);
        userService.updateResearcherCurrentOrganisationUnitIfBound(
            membershipToUpdate.getPersonInvolved().getId());
    }

    @Override
    public void updateEmployment(Integer involvementId, EmploymentDTO employment) {
        var employmentToUpdate = (Employment) findOne(involvementId);

        var role = multilingualContentService.getMultilingualContent(employment.getRole());

        clearCommonCollections(employmentToUpdate);
        employmentToUpdate.getRole().clear();

        setCommonFields(employmentToUpdate, employment);
        employmentToUpdate.setEmploymentPosition(employment.getEmploymentPosition());
        employmentToUpdate.setRole(role);

        involvementRepository.save(employmentToUpdate);
        userService.updateResearcherCurrentOrganisationUnitIfBound(
            employmentToUpdate.getPersonInvolved().getId());
        personService.indexPerson(employmentToUpdate.getPersonInvolved());

        applicationEventPublisher.publishEvent(new PersonEmploymentOUHierarchyStructureChangedEvent(
            employmentToUpdate.getPersonInvolved().getId()));
    }

    @Override
    public void deleteInvolvement(Integer involvementId) {
        var involvementToDelete = findOne(involvementId);
        var person = involvementToDelete.getPersonInvolved();

        involvementToDelete.getProofs()
            .forEach(proof -> documentFileService.deleteDocumentFile(proof.getServerFilename()));

        delete(involvementId);
        userService.updateResearcherCurrentOrganisationUnitIfBound(person.getId());

        if (involvementToDelete instanceof Employment) {
            applicationEventPublisher.publishEvent(
                new PersonEmploymentOUHierarchyStructureChangedEvent(
                    involvementToDelete.getPersonInvolved().getId()));
        }

        person.removeInvolvement(involvementToDelete);
        personService.save(person);
        personService.indexPerson(person);
    }

    @Override
    public void endEmployment(Integer institutionId, Integer personId) {
        var employment =
            involvementRepository.findActiveEmploymentForPersonAndInstitution(institutionId,
                personId);
        if (employment.isEmpty()) {
            throw new NotFoundException(
                "Employment with that person and institution does not exist.");
        }

        employment.get().setDateTo(LocalDate.now());
        employmentRepository.save(employment.get());
        personService.indexPerson(employment.get().getPersonInvolved());

        applicationEventPublisher.publishEvent(new PersonEmploymentOUHierarchyStructureChangedEvent(
            employment.get().getPersonInvolved().getId()));
    }

    @Override
    public EmploymentTitle getCurrentEmploymentTitle(Integer personId) {
        return employmentRepository.findByPersonInvolvedId(personId).stream()
            .filter(employment -> employment.getDateTo() == null)
            .map(Employment::getEmploymentPosition)
            .map(this::mapToEmploymentTitle)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private EmploymentTitle mapToEmploymentTitle(EmploymentPosition position) {
        if (Objects.isNull(position)) {
            return null;
        }

        return switch (position) {
            case FULL_PROFESSOR -> EmploymentTitle.FULL_PROFESSOR;
            case ASSISTANT_PROFESSOR -> EmploymentTitle.ASSISTANT_PROFESSOR;
            case ASSOCIATE_PROFESSOR -> EmploymentTitle.ASSOCIATE_PROFESSOR;
            case SCIENTIFIC_COLLABORATOR -> EmploymentTitle.SCIENTIFIC_COLLABORATOR;
            case SENIOR_SCIENTIFIC_COLLABORATOR -> EmploymentTitle.SENIOR_SCIENTIFIC_COLLABORATOR;
            case SCIENTIFIC_ADVISOR -> EmploymentTitle.SCIENTIFIC_ADVISOR;
            case PROFESSOR_EMERITUS -> EmploymentTitle.PROFESSOR_EMERITUS;
            case RETIRED_PROFESSOR -> EmploymentTitle.RETIRED_PROFESSOR;
            case PROFESSOR_ENGINEER_HABILITATED -> EmploymentTitle.PROFESSOR_ENGINEER_HABILITATED;
            default -> null;
        };
    }

    private void setCommonFields(Involvement involvement, InvolvementDTO commonFields) {
        if (Objects.nonNull(commonFields.getOrganisationUnitId()) &&
            commonFields.getOrganisationUnitId() > 0) {
            var organisationUnit =
                organisationUnitService.findOrganisationUnitById(
                    commonFields.getOrganisationUnitId());
            involvement.setOrganisationUnit(organisationUnit);
        } else {
            var affiliationStatements = multilingualContentService.getMultilingualContent(
                commonFields.getAffiliationStatement());
            involvement.setAffiliationStatement(affiliationStatements);
        }

        involvement.setDateFrom(commonFields.getDateFrom());
        involvement.setDateTo(commonFields.getDateTo());
        involvement.setApproveStatus(ApproveStatus.APPROVED);
        involvement.setInvolvementType(commonFields.getInvolvementType());
    }

    private void clearCommonCollections(Involvement involvement) {
        involvement.getAffiliationStatement().clear();
        involvement.getProofs().clear();
    }
}
