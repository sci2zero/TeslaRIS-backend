package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import rs.teslaris.core.model.person.Person;
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
    public List<PersonResponseDTO> readPersonAssessmentResearchAreaForCommission(
        Integer commissionId, String code) {
        var involvementTypes = Set.of(InvolvementType.EMPLOYED_AT, InvolvementType.HIRED_BY,
            InvolvementType.CANDIDATE);
        var organisationUnitId = userRepository.findOUIdForCommission(commissionId);

        Set<Person> persons =
            assessmentResearchAreaRepository.findPersonsForAssessmentResearchArea(commissionId,
                code, involvementTypes, organisationUnitId);

        return persons.stream()
            .map(PersonConverter::toDTO)
            .toList();
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
        var commission = commissionService.findOne(commissionId);

        if (researchArea.isPresent()) {
            researchArea.get().setResearchAreaCode(researchAreaCode);
            commission.getExcludedResearchers().remove(researchArea.get().getPerson());
            researchArea.get().setCommission(commission);
            save(researchArea.get());
            commissionService.save(commission);
            return;
        }

        var person = personService.findOne(personId);
        commission.getExcludedResearchers().remove(person);

        var newResearchArea = new AssessmentResearchArea();
        newResearchArea.setPerson(person);
        newResearchArea.setResearchAreaCode(researchAreaCode);
        newResearchArea.setCommission(commission);

        save(newResearchArea);
        commissionService.save(commission);
    }

    @Override
    public void deletePersonAssessmentResearchArea(Integer personId) {
        var researchAreaToDelete = assessmentResearchAreaRepository.findForPersonId(personId);
        researchAreaToDelete.ifPresent(assessmentResearchAreaRepository::delete);
    }

    @Override
    public void removePersonAssessmentResearchAreaForCommission(Integer personId,
                                                                Integer commissionId) {
        var commission = commissionService.findOne(commissionId);
        commission.getExcludedResearchers().add(personService.findOne(personId));
        commissionService.save(commission);
    }

    @Override
    protected JpaRepository<AssessmentResearchArea, Integer> getEntityRepository() {
        return assessmentResearchAreaRepository;
    }
}
