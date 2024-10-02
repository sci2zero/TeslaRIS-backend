package rs.teslaris.core.converter.assessment;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.assessment.AssessmentRulebookResponseDTO;
import rs.teslaris.core.model.assessment.AssessmentRulebook;

public class AssessmentRulebookConverter {

    public static AssessmentRulebookResponseDTO toDTO(AssessmentRulebook assessmentRulebook) {
        return new AssessmentRulebookResponseDTO(
            assessmentRulebook.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(assessmentRulebook.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(
                assessmentRulebook.getDescription()),
            assessmentRulebook.getIssueDate(),
            Objects.nonNull(assessmentRulebook.getPdfFile()) ?
                DocumentFileConverter.toDTO(assessmentRulebook.getPdfFile()) : null,
            Objects.nonNull(assessmentRulebook.getPublisher()) ?
                assessmentRulebook.getPublisher().getId() : null,
            Objects.nonNull(assessmentRulebook.getAssessmentMeasure()) ?
                assessmentRulebook.getAssessmentMeasure().getId() : null
        );
    }
}
