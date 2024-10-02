package rs.teslaris.core.service.interfaces.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.assessment.AssessmentRulebookDTO;
import rs.teslaris.core.dto.assessment.AssessmentRulebookResponseDTO;
import rs.teslaris.core.model.assessment.AssessmentRulebook;
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
