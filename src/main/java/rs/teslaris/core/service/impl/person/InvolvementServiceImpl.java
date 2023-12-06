package rs.teslaris.core.service.impl.person;

import java.util.HashSet;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.person.InvolvementConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;
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
    public Employment addEmployment(Integer personId, EmploymentDTO employment) {
        var personInvolved = personService.findOne(personId);

        var role = multilingualContentService.getMultilingualContent(employment.getRole());

        var newEmployment = new Employment();
        setCommonFields(newEmployment, employment);
        newEmployment.setEmploymentPosition(employment.getEmploymentPosition());
        newEmployment.setRole(role);

        personInvolved.addInvolvement(newEmployment);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);

        return involvementRepository.save(newEmployment);
    }

    @Override
    public void addInvolvementProofs(List<DocumentFileDTO> proofs, Integer involvementId) {
        var involvement = findOne(involvementId);
        proofs.forEach(proof -> {
            var documentFile = documentFileService.saveNewDocument(proof, true);
            involvement.getProofs().add(documentFile);
            involvementRepository.save(involvement);
        });
    }

    @Override
    public void deleteProof(Integer proofId, Integer involvementId) {
        var involvement = findOne(involvementId);
        var documentFile = documentFileService.findDocumentFileById(proofId);

        involvement.setDeleted(true);
        save(involvement);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
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
    }

    @Override
    public void deleteInvolvement(Integer involvementId) {
        var involvementToDelete = findOne(involvementId);
//        TODO: Do i need to delete those involvments or just logicaly avoid (soft delete)
        var personId = involvementToDelete.getPersonInvolved().getId();
        involvementToDelete.getPersonInvolved().removeInvolvement(involvementToDelete);

        involvementToDelete.getProofs()
            .forEach(proof -> documentFileService.deleteDocumentFile(proof.getServerFilename()));

        delete(involvementId);
        userService.updateResearcherCurrentOrganisationUnitIfBound(personId);
    }


    private void setCommonFields(Involvement involvement, InvolvementDTO commonFields) {
        var organisationUnit =
            organisationUnitService.findOrganisationUnitById(
                commonFields.getOrganisationUnitId());

        var affiliationStatements = multilingualContentService.getMultilingualContent(
            commonFields.getAffiliationStatement());

        involvement.setDateFrom(commonFields.getDateFrom());
        involvement.setDateTo(commonFields.getDateTo());
        involvement.setApproveStatus(ApproveStatus.APPROVED);
        involvement.setProofs(new HashSet<>());
        involvement.setInvolvementType(commonFields.getInvolvementType());
        involvement.setAffiliationStatement(affiliationStatements);
        involvement.setOrganisationUnit(organisationUnit);
    }

    private void clearCommonCollections(Involvement involvement) {
        involvement.getAffiliationStatement().clear();
        involvement.getProofs().clear();
    }
}
