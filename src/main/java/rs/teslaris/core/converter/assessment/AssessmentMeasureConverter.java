package rs.teslaris.core.converter.assessment;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.assessment.AssessmentMeasureDTO;
import rs.teslaris.core.model.assessment.AssessmentMeasure;

public class AssessmentMeasureConverter {

    public static AssessmentMeasureDTO toDTO(AssessmentMeasure assessmentMeasure) {
        return new AssessmentMeasureDTO(
            assessmentMeasure.getId(),
            assessmentMeasure.getFormalDescriptionOfRule(),
            assessmentMeasure.getCode(), assessmentMeasure.getValue(),
            MultilingualContentConverter.getMultilingualContentDTO(assessmentMeasure.getTitle()));
    }
}
