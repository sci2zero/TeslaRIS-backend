package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentResearchAreaService;
import rs.teslaris.core.assessment.util.ResearchAreasConfigurationLoader;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class AssessmentResearchAreaServiceImpl extends JPAServiceImpl<AssessmentResearchArea>
    implements AssessmentResearchAreaService {

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    private final PersonService personService;


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
    public void deletePersonAssessmentResearchArea(Integer personId) {
        var researchAreaToDelete = assessmentResearchAreaRepository.findForPersonId(personId);
        researchAreaToDelete.ifPresent(assessmentResearchAreaRepository::delete);
    }

    @Override
    protected JpaRepository<AssessmentResearchArea, Integer> getEntityRepository() {
        return assessmentResearchAreaRepository;
    }
}
