package rs.teslaris.assessment.service.impl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.AssessmentMeasureConverter;
import rs.teslaris.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.assessment.ruleengine.AssessmentPointsRuleEngine;
import rs.teslaris.assessment.ruleengine.AssessmentPointsScalingRuleEngine;
import rs.teslaris.assessment.service.interfaces.AssessmentMeasureService;
import rs.teslaris.assessment.service.interfaces.AssessmentRulebookService;
import rs.teslaris.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class AssessmentMeasureServiceImpl extends JPAServiceImpl<AssessmentMeasure> implements
    AssessmentMeasureService {

    private final AssessmentMeasureRepository assessmentMeasureRepository;

    private final AssessmentRulebookService assessmentRulebookService;

    private final MultilingualContentService multilingualContentService;


    @Override
    public Page<AssessmentMeasureDTO> searchAssessmentMeasures(Pageable pageable,
                                                               String searchExpression) {
        return assessmentMeasureRepository.searchAssessmentMeasures(pageable, searchExpression)
            .map(AssessmentMeasureConverter::toDTO);
    }

    @Override
    protected JpaRepository<AssessmentMeasure, Integer> getEntityRepository() {
        return assessmentMeasureRepository;
    }

    @Override
    public AssessmentMeasureDTO readAssessmentMeasureById(
        Integer assessmentMeasureId) {
        return AssessmentMeasureConverter.toDTO(findOne(assessmentMeasureId));
    }

    @Override
    public AssessmentMeasure createAssessmentMeasure(
        AssessmentMeasureDTO assessmentMeasure) {
        var newAssessmentMeasure = new AssessmentMeasure();

        setCommonFields(newAssessmentMeasure, assessmentMeasure);

        return save(newAssessmentMeasure);
    }

    @Override
    public void updateAssessmentMeasure(Integer assessmentMeasureId,
                                        AssessmentMeasureDTO assessmentMeasure) {
        var assessmentMeasureToUpdate = findOne(assessmentMeasureId);

        setCommonFields(assessmentMeasureToUpdate, assessmentMeasure);

        save(assessmentMeasureToUpdate);
    }

    private void setCommonFields(AssessmentMeasure assessmentMeasure,
                                 AssessmentMeasureDTO assessmentMeasureDTO) {
        if (!getRuleEngineRules(AssessmentPointsRuleEngine.class).contains(
            assessmentMeasureDTO.pointRule()) ||
            !getRuleEngineRules(AssessmentPointsScalingRuleEngine.class).contains(
                assessmentMeasureDTO.scalingRule())) {
            throw new NotFoundException("Provided rule does not exist.");
        }

        assessmentMeasure.setPointRule(assessmentMeasureDTO.pointRule());
        assessmentMeasure.setScalingRule(assessmentMeasureDTO.scalingRule());

        assessmentMeasure.setCode(assessmentMeasureDTO.code());
        assessmentMeasure.setTitle(
            multilingualContentService.getMultilingualContent(assessmentMeasureDTO.title()));

        var rulebook =
            assessmentRulebookService.findOne(assessmentMeasureDTO.assessmentRulebookId());
        assessmentMeasure.setRulebook(rulebook);
        if (!rulebook.getAssessmentMeasures().contains(assessmentMeasure)) {
            rulebook.getAssessmentMeasures().add(assessmentMeasure);
        }
    }

    @Override
    public void deleteAssessmentMeasure(Integer assessmentMeasureId) {
        var assessmentMeasureToDelete = findOne(assessmentMeasureId);

        if (Objects.nonNull(assessmentMeasureToDelete.getRulebook())) {
            assessmentMeasureToDelete.getRulebook().getAssessmentMeasures()
                .remove(assessmentMeasureToDelete);
        }

        delete(assessmentMeasureId);
    }

    @Override
    public List<String> listAllPointRules() {
        return getRuleEngineRules(AssessmentPointsRuleEngine.class);
    }

    @Override
    public List<String> listAllScalingRules() {
        return getRuleEngineRules(AssessmentPointsScalingRuleEngine.class);
    }

    @Override
    public List<String> listAllGroupCodes() {
        return ClassificationPriorityMapping.getAssessmentGroups();
    }

    private <T> List<String> getRuleEngineRules(Class<T> clazz) {
        return Arrays.stream(clazz.getMethods()).map(Method::getName)
            .filter(name -> name.contains("Rulebook")).sorted().collect(
                Collectors.toList());
    }
}
