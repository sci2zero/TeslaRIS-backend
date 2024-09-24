package rs.teslaris.core.converter.assessment;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.assessment.AssessmentClassificationDTO;
import rs.teslaris.core.model.assessment.AssessmentClassification;

public class AssessmentClassificationConverter {

    public static AssessmentClassificationDTO toDTO(
        AssessmentClassification assessmentClassification) {
        return new AssessmentClassificationDTO(
            assessmentClassification.getFormalDescriptionOfRule(),
            assessmentClassification.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(
                assessmentClassification.getTitle()));
    }
}
