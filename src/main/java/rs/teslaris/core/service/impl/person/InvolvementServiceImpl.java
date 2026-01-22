package rs.teslaris.core.service.impl.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.person.InvolvementConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.InternalIdentifierMigrationDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.ExtraEmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.repository.person.EmploymentRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.impl.person.worker.EmploymentMigrationWorker;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.CollectionOperations;

@Slf4j
@Service
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

    private final EmploymentMigrationWorker employmentMigrationWorker;


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
    @Transactional(readOnly = true)
    public <T extends Involvement, R> R getInvolvement(Integer involvementId,
                                                       Class<T> involvementClass) {
        var involvement = findOne(involvementId);
        return (R) InvolvementConverter.toDTO(involvementClass.cast(involvement));
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional
    public Employment addEmployment(Integer personId, EmploymentDTO employment) {
        var personInvolved = personService.findOne(personId);

        var newEmployment = new Employment();
        setCommonFields(newEmployment, employment);
        newEmployment.setEmploymentPosition(employment.getEmploymentPosition());

        if (Objects.nonNull(employment.getRole())) {
            newEmployment.setRole(
                multilingualContentService.getMultilingualContent(employment.getRole()));
        }

        personInvolved.addInvolvement(newEmployment);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);
        personService.indexPerson(personInvolved);

        var savedEmployment = involvementRepository.save(newEmployment);

        applicationEventPublisher.publishEvent(
            new PersonEmploymentOUHierarchyStructureChangedEvent(personId));

        return savedEmployment;
    }

    @Override
    @Transactional
    public EmploymentDTO migrateEmployment(EmploymentMigrationDTO employmentMigrationRequest) {
        var employment =
            employmentMigrationWorker.performLegacyMigration(employmentMigrationRequest);

        return InvolvementConverter.toDTO(employment);
    }

    @Override
    public void migrateEmployment(List<ExtraEmploymentMigrationDTO> request,
                                  LocalDate initialMigrationDate,
                                  Integer additionalInstitutionToLook) {
        var handledInstitutionIds = new HashSet<Integer>();

        if (Objects.nonNull(additionalInstitutionToLook)) {
            handledInstitutionIds.addAll(
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                    additionalInstitutionToLook));
        }

        var handledPersonIds = new HashSet<Integer>();

        for (var migration : request) {
            log.info("Handling migration for {} {} with accounting ID {}",
                migration.personName().getFirstname(),
                migration.personName().getLastname(),
                migration.personAccountingId()
            );

            employmentMigrationWorker.processSingleMigration(migration, handledInstitutionIds,
                handledPersonIds);
        }

        var employmentIdsForDeletion = new ArrayList<Integer>();
        var personIdsForDeletion = new ArrayList<Integer>();

        for (var institutionId : handledInstitutionIds) {
            log.info("Cleaning up alumni for institution with ID {}", institutionId);

            for (var employment :
                employmentRepository.findByEmploymentInstitutionId(institutionId)) {
                employmentMigrationWorker.cleanupAlumni(employment, handledPersonIds,
                    employmentIdsForDeletion, personIdsForDeletion, initialMigrationDate);
            }
        }

        log.info("STALE DATA CLEANUP SUMMARY:");
        log.info("Employment records marked for deletion: {} - IDs: {}",
            employmentIdsForDeletion.size(), employmentIdsForDeletion);
        log.info("Person records marked for deletion: {} - IDs: {}",
            personIdsForDeletion.size(), personIdsForDeletion);
    }

    @Override
    @Transactional
    public void migrateEmployeeInternalIdentifiers(InternalIdentifierMigrationDTO dto) {
        dto.oldToInternalIdMapping().forEach((oldId, internalId) -> {
            var person = personService.findPersonByOldId(oldId);
            if (Objects.isNull(person)) {
                log.warn("Person with old ID {} does not exist.", oldId);
                return;
            }

            if (dto.accountingIds()) {
                person.getAccountingIds()
                    .add(String.valueOf(internalId));
            } else {
                person.getInternalIdentifiers()
                    .add(String.valueOf(internalId));
            }

            personService.save(person);
        });
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
    public DocumentFileResponseDTO updateProof(Integer proofId, Integer involvementId,
                                               DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
            involvement.getAffiliationStatement().clear();
        } else {
            involvement.setOrganisationUnit(null);
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

    @Async("taskExecutor")
    @Transactional
    public void addExternalInvolvementToBoardMember(Integer personId,
                                                    List<MultilingualContentDTO> externalInstitutionName,
                                                    String employmentTitle) {
        if (employmentRepository.findExternalByPersonInvolvedId(personId).stream()
            .anyMatch(employment ->
                CollectionOperations.hasCaseInsensitiveMatch(
                    externalInstitutionName.stream().map(MultilingualContentDTO::getContent)
                        .collect(Collectors.toSet()), employment.getAffiliationStatement().stream()
                        .map(MultiLingualContent::getContent).collect(Collectors.toSet())))) {
            return;
        }

        if (employmentTitle.equals("ACADEMICIAN")) {
            employmentTitle = "FULL_PROFESSOR";
        }

        var finalEmploymentTitle = employmentTitle;
        addEmployment(personId, new EmploymentDTO() {{
            setInvolvementType(InvolvementType.EMPLOYED_AT);
            setAffiliationStatement(externalInstitutionName);
            setEmploymentPosition(EmploymentPosition.valueOf(finalEmploymentTitle));
        }});
    }

    @Override
    @Transactional(readOnly = true)
    public List<List<MultilingualContentDTO>> getExternalInstitutionSuggestions(Integer personId) {
        return employmentRepository.findExternalByPersonInvolvedId(personId).stream().map(
            employment -> MultilingualContentConverter.getMultilingualContentDTO(
                employment.getAffiliationStatement())).toList();
    }

    @Scheduled(cron = "0 0 3 * * ?") // Every day at 03:00 AM
    protected void reindexEmploymentInformationForEmploymentsThatExpireToday() {
        employmentRepository.findEmploymentsExpiringToday().forEach(
            employment -> applicationEventPublisher.publishEvent(
                new PersonEmploymentOUHierarchyStructureChangedEvent(
                    employment.getPersonInvolved().getId())));
    }
}
