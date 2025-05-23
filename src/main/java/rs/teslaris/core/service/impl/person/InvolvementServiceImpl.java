package rs.teslaris.core.service.impl.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.person.InvolvementConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
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
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@Transactional
@RequiredArgsConstructor
public class InvolvementServiceImpl extends JPAServiceImpl<Involvement>
    implements InvolvementService {

    private final PersonService personService;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;

    private final InvolvementRepository involvementRepository;

    private final DocumentFileService documentFileService;

    private final EmploymentRepository employmentRepository;

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

            directAndIndirectEmployments.add(InvolvementConverter.toDTO(employment));

            var employmentOU = employment.getOrganisationUnit();
            organisationUnitService.getSuperOUsHierarchyRecursive(employmentOU.getId())
                .forEach(indirectSuperEmployment -> {
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
    public Employment addEmployment(Integer personId, EmploymentDTO employment) {
        var personInvolved = personService.findOne(personId);

        var role = multilingualContentService.getMultilingualContent(employment.getRole());

        var newEmployment = new Employment();
        setCommonFields(newEmployment, employment);
        newEmployment.setEmploymentPosition(employment.getEmploymentPosition());
        newEmployment.setRole(role);

        personInvolved.addInvolvement(newEmployment);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);
        personService.indexPerson(personInvolved, personInvolved.getId());

        return involvementRepository.save(newEmployment);
    }

    @Override
    public EmploymentDTO migrateEmployment(EmploymentMigrationDTO employmentMigrationRequest) {
        Person person = resolvePerson(employmentMigrationRequest);
        OrganisationUnit organisationUnit = resolveOrganisationUnit(employmentMigrationRequest);

        var existingEmployment = findExistingEmployment(person, organisationUnit,
            employmentMigrationRequest.employmentPosition());

        Employment employment;
        if (existingEmployment.isPresent()) {
            employment = updateExistingEmployment((Employment) existingEmployment.get(),
                employmentMigrationRequest);
        } else {
            employment =
                createNewMigratedEmployment(person, organisationUnit, employmentMigrationRequest);
            closeOtherOpenEmployments(person, organisationUnit, employment);
        }

        personService.save(person);
        userService.updateResearcherCurrentOrganisationUnitIfBound(person.getId());
        return InvolvementConverter.toDTO(employment);
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

    private Optional<Involvement> findExistingEmployment(Person person, OrganisationUnit unit,
                                                         EmploymentPosition position) {
        return person.getInvolvements().stream()
            .filter(involvement ->
                involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) &&
                    involvement.getOrganisationUnit().getId().equals(unit.getId()) &&
                    ((Employment) involvement).getEmploymentPosition().equals(position))
            .findAny();
    }

    private Employment updateExistingEmployment(Employment employment,
                                                EmploymentMigrationDTO request) {
        employment.setDateFrom(request.employmentStartDate());
        return involvementRepository.save(employment);
    }

    private Employment createNewMigratedEmployment(Person person, OrganisationUnit unit,
                                                   EmploymentMigrationDTO request) {
        var employment = new Employment();
        employment.setDateFrom(request.employmentStartDate());
        employment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        employment.setOrganisationUnit(unit);
        employment.setEmploymentPosition(request.employmentPosition());
        employment.setApproveStatus(ApproveStatus.APPROVED);

        person.addInvolvement(employment);
        Employment saved = employmentRepository.save(employment);
        personService.indexPerson(person, person.getId());
        return saved;
    }

    private void closeOtherOpenEmployments(Person person, OrganisationUnit unit,
                                           Employment newEmployment) {
        person.getInvolvements().stream()
            .filter(involvement ->
                involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) &&
                    involvement.getOrganisationUnit().getId().equals(unit.getId()) &&
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
        personService.indexPerson(employmentToUpdate.getPersonInvolved(),
            employmentToUpdate.getPersonInvolved().getId());
    }

    @Override
    public void deleteInvolvement(Integer involvementId) {
        var involvementToDelete = findOne(involvementId);
        var personId = involvementToDelete.getPersonInvolved().getId();

        involvementToDelete.getProofs()
            .forEach(proof -> documentFileService.deleteDocumentFile(proof.getServerFilename()));

        delete(involvementId);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);
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
        personService.indexPerson(employment.get().getPersonInvolved(), personId);
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
