package rs.teslaris.core.assessment.converter;

import rs.teslaris.core.assessment.dto.AssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class AssessmentClassificationConverter {

    public static AssessmentClassificationDTO toDTO(
        AssessmentClassification assessmentClassification) {
        return new AssessmentClassificationDTO(
            assessmentClassification.getId(),
            assessmentClassification.getFormalDescriptionOfRule(),
            assessmentClassification.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(
                assessmentClassification.getTitle()),
            assessmentClassification.getApplicableTypes().stream().toList());
    }
}
