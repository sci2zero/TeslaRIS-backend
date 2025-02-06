package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentResearchAreaService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.util.ResearchAreasConfigurationLoader;
import rs.teslaris.core.converter.person.PersonConverter;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentResearchAreaServiceImpl extends JPAServiceImpl<AssessmentResearchArea>
    implements AssessmentResearchAreaService {

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    private final PersonService personService;

    private final CommissionService commissionService;

    private final UserRepository userRepository;


    @Override
    public List<AssessmentResearchAreaDTO> readAllAssessmentResearchAreas() {
        return ResearchAreasConfigurationLoader.fetchAllAssessmentResearchAreas();
    }

    @Override
    public AssessmentResearchAreaDTO readPersonAssessmentResearchArea(Integer personId) {
        var researchArea = assessmentResearchAreaRepository.findForPersonId(personId).orElse(null);
        return Objects.nonNull(researchArea) ?
            ResearchAreasConfigurationLoader.fetchAssessmentResearchAreaByCode(
                researchArea.getResearchAreaCode()).get() : null;
    }

    @Override
    public List<PersonResponseDTO> readPersonAssessmentResearchAreaForCommission(Integer personId,
                                                                                 Integer commissionId,
                                                                                 String code) {
        var organisationUnitId = userRepository.findOUIdForCommission(commissionId);
        var persons =
            assessmentResearchAreaRepository.findAllForPersonIdAndCommissionIdAndCode(personId,
                commissionId, code);
        persons.addAll(assessmentResearchAreaRepository.findAllForPersonIdAndCode(personId, code));

        return persons.stream().filter(person -> {
            for (var involvement : person.getInvolvements()) {
                if ((involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                    involvement.getInvolvementType().equals(InvolvementType.HIRED_BY) ||
                    involvement.getInvolvementType().equals(InvolvementType.CANDIDATE)) &&
                    involvement.getOrganisationUnit().getId().equals(organisationUnitId)) {
                    return true;
                }
            }
            return false;
        }).map(PersonConverter::toDTO).toList();
    }

    @Override
    public void setPersonAssessmentResearchArea(Integer personId, String researchAreaCode) {
        if (!ResearchAreasConfigurationLoader.codeExists(researchAreaCode)) {
            throw new NotFoundException(
                "Research area code " + researchAreaCode + " does not exist.");
        }

        var researchArea = assessmentResearchAreaRepository.findForPersonId(personId);

        if (researchArea.isPresent()) {
            researchArea.get().setResearchAreaCode(researchAreaCode);
            save(researchArea.get());
            return;
        }

        var newResearchArea = new AssessmentResearchArea();
        newResearchArea.setPerson(personService.findOne(personId));
        newResearchArea.setResearchAreaCode(researchAreaCode);
        save(newResearchArea);
    }

    @Override
    public void setPersonAssessmentResearchAreaForCommission(Integer personId,
                                                             String researchAreaCode,
                                                             Integer commissionId) {
        if (!ResearchAreasConfigurationLoader.codeExists(researchAreaCode)) {
            throw new NotFoundException(
                "Research area code " + researchAreaCode + " does not exist.");
        }

        var researchArea =
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId);

        if (researchArea.isPresent()) {
            researchArea.get().setResearchAreaCode(researchAreaCode);
            researchArea.get().setCommission(commissionService.findOne(commissionId));
            save(researchArea.get());
            return;
        }

        var newResearchArea = new AssessmentResearchArea();
        newResearchArea.setPerson(personService.findOne(personId));
        newResearchArea.setResearchAreaCode(researchAreaCode);
        newResearchArea.setCommission(commissionService.findOne(commissionId));

        save(newResearchArea);
    }

    @Override
    public void deletePersonAssessmentResearchArea(Integer personId) {
        var researchAreaToDelete = assessmentResearchAreaRepository.findForPersonId(personId);
        researchAreaToDelete.ifPresent(assessmentResearchAreaRepository::delete);
    }

    @Override
    public void removePersonAssessmentResearchAreaForCommission(Integer personId,
                                                                Integer commissionId) {
        var researchAreaToRemove =
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId);
        researchAreaToRemove.ifPresent(researchArea -> {
            researchArea.setResearchAreaCode(null);
            assessmentResearchAreaRepository.save(researchArea);
        });
    }

    @Override
    protected JpaRepository<AssessmentResearchArea, Integer> getEntityRepository() {
        return assessmentResearchAreaRepository;
    }
}
