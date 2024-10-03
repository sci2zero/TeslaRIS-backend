package rs.teslaris.core.assessment.converter;

import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class AssessmentMeasureConverter {

    public static AssessmentMeasureDTO toDTO(AssessmentMeasure assessmentMeasure) {
        return new AssessmentMeasureDTO(
            assessmentMeasure.getId(),
            assessmentMeasure.getFormalDescriptionOfRule(),
            assessmentMeasure.getCode(), assessmentMeasure.getValue(),
            MultilingualContentConverter.getMultilingualContentDTO(assessmentMeasure.getTitle()));
    }
}
