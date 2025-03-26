package rs.teslaris.assessment.converter;

import java.util.Objects;
import rs.teslaris.assessment.dto.AssessmentRulebookResponseDTO;
import rs.teslaris.assessment.model.AssessmentRulebook;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;

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
            Objects.nonNull(assessmentRulebook.getPublisher()) ?
                MultilingualContentConverter.getMultilingualContentDTO(
                    assessmentRulebook.getPublisher().getName()) : null,
            assessmentRulebook.getIsDefault()
        );
    }
}
