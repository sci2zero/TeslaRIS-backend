package rs.teslaris.core.assessment.converter;

import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.assessment.dto.AssessmentRulebookResponseDTO;
import rs.teslaris.core.assessment.model.AssessmentRulebook;
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
            assessmentRulebook.getAssessmentMeasures().stream()
                .map(AssessmentMeasureConverter::toDTO).collect(
                    Collectors.toList())
        );
    }
}
