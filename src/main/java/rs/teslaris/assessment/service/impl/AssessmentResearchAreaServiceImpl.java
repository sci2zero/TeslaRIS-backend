package rs.teslaris.assessment.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.assessment.model.AssessmentResearchArea;
import rs.teslaris.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.assessment.service.interfaces.AssessmentResearchAreaService;
import rs.teslaris.assessment.util.ResearchAreasConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Traceable
public class AssessmentResearchAreaServiceImpl extends JPAServiceImpl<AssessmentResearchArea>
    implements AssessmentResearchAreaService {

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    private final PersonService personService;

    private final CommissionRepository commissionRepository;

    private final UserRepository userRepository;

    private final PersonIndexRepository personIndexRepository;

    private final ResearchAreaRepository researchAreaRepository;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    public List<AssessmentResearchAreaDTO> readAllAssessmentResearchAreas() {
        return ResearchAreasConfigurationLoader.fetchAllAssessmentResearchAreas();
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentResearchAreaDTO readPersonAssessmentResearchArea(Integer personId) {
        var researchArea = assessmentResearchAreaRepository.findForPersonId(personId).orElse(null);

        var researchAreaResponse = Objects.nonNull(researchArea) ?
            ResearchAreasConfigurationLoader.fetchAssessmentResearchAreaByCode(
                researchArea.getResearchAreaCode()).get() : null;

        if (Objects.nonNull(researchAreaResponse)) {
            researchAreaResponse.setResearchSubAreas(
                researchAreaRepository.getResearchAreaByIdsIn(researchArea.getResearchSubAreaIds())
                    .stream().map(ResearchAreaConverter::toDTO).toList());
        }

        return researchAreaResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonIndex> readPersonAssessmentResearchAreaForCommission(
        Integer commissionId, String code, Pageable pageable) {
        var organisationUnitId = userRepository.findOUIdForCommission(commissionId);

        var personIds =
            assessmentResearchAreaRepository.findPersonsForAssessmentResearchArea(commissionId,
                code, organisationUnitId);

        return personIndexRepository.findByDatabaseIdIn(personIds, pageable);
    }

    @Override
    @Transactional
    public void setPersonAssessmentResearchArea(Integer personId, String researchAreaCode,
                                                List<Integer> researchSubAreaIds) {
        if (!ResearchAreasConfigurationLoader.codeExists(researchAreaCode)) {
            throw new NotFoundException(
                "Research area code " + researchAreaCode + " does not exist.");
        }

        if (Objects.isNull(researchSubAreaIds)) {
            researchSubAreaIds = Collections.emptyList();
        }

        var researchArea = assessmentResearchAreaRepository.findForPersonId(personId);

        if (researchArea.isPresent()) {
            researchArea.get().setResearchAreaCode(researchAreaCode);
            researchArea.get().setResearchSubAreaIds(new HashSet<>(researchSubAreaIds));
            save(researchArea.get());
            return;
        }

        var newResearchArea = new AssessmentResearchArea();
        newResearchArea.setPerson(personService.findOne(personId));
        newResearchArea.setResearchAreaCode(researchAreaCode);
        newResearchArea.setResearchSubAreaIds(new HashSet<>(researchSubAreaIds));
        save(newResearchArea);

        applicationEventPublisher.publishEvent(
            new ResearcherPointsReindexingEvent(List.of(personId)));
    }

    @Override
    @Transactional
    public void setPersonAssessmentResearchAreaForCommission(Integer personId,
                                                             String researchAreaCode,
                                                             Integer commissionId) {
        if (!ResearchAreasConfigurationLoader.codeExists(researchAreaCode)) {
            throw new NotFoundException(
                "Research area code " + researchAreaCode + " does not exist.");
        }

        var researchArea =
            assessmentResearchAreaRepository.findForPersonIdAndCommissionId(personId, commissionId);
        var commission = commissionRepository.getReferenceById(commissionId);

        if (researchArea.isPresent()) {
            researchArea.get().setResearchAreaCode(researchAreaCode);
            commissionRepository.removeExcludedResearcher(commissionId, personId);
            researchArea.get().setCommission(commission);
            save(researchArea.get());

            applicationEventPublisher.publishEvent(
                new ResearcherPointsReindexingEvent(List.of(personId)));

            return;
        }

        var person = personService.findOne(personId);
        commissionRepository.removeExcludedResearcher(commissionId, personId);

        var newResearchArea = new AssessmentResearchArea();
        newResearchArea.setPerson(person);
        newResearchArea.setResearchAreaCode(researchAreaCode);
        newResearchArea.setCommission(commission);

        save(newResearchArea);

        applicationEventPublisher.publishEvent(
            new ResearcherPointsReindexingEvent(List.of(personId)));
    }

    @Override
    @Transactional
    public void deletePersonAssessmentResearchArea(Integer personId) {
        var researchAreaToDelete = assessmentResearchAreaRepository.findForPersonId(personId);
        researchAreaToDelete.ifPresent(assessmentResearchAreaRepository::delete);

        applicationEventPublisher.publishEvent(
            new ResearcherPointsReindexingEvent(List.of(personId)));
    }

    @Override
    @Transactional
    public void removePersonAssessmentResearchAreaForCommission(Integer personId,
                                                                Integer commissionId) {
        commissionRepository.addExcludedResearcher(commissionId, personId);

        applicationEventPublisher.publishEvent(
            new ResearcherPointsReindexingEvent(List.of(personId)));
    }

    @Override
    protected JpaRepository<AssessmentResearchArea, Integer> getEntityRepository() {
        return assessmentResearchAreaRepository;
    }
}
