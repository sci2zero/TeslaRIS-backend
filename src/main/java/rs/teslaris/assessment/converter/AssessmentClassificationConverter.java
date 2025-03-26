package rs.teslaris.assessment.converter;

import rs.teslaris.assessment.dto.AssessmentClassificationDTO;
import rs.teslaris.assessment.model.AssessmentClassification;
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
