package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.AssessmentRulebookDTO;
import rs.teslaris.core.assessment.dto.AssessmentRulebookResponseDTO;
import rs.teslaris.core.assessment.model.AssessmentRulebook;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface AssessmentRulebookService extends JPAService<AssessmentRulebook> {

    Page<AssessmentRulebookResponseDTO> readAllAssessmentRulebooks(Pageable pageable);

    AssessmentRulebookResponseDTO readAssessmentRulebookById(Integer assessmentRulebookId);

    AssessmentRulebook createAssessmentRulebook(AssessmentRulebookDTO assessmentRulebookDTO);

    void updateAssessmentRulebook(Integer assessmentRulebookId,
                                  AssessmentRulebookDTO assessmentRulebookDTO);

    void deleteAssessmentRulebook(Integer assessmentRulebookId);
}