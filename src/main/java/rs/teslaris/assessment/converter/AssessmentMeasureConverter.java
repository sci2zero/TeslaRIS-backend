package rs.teslaris.assessment.converter;

import java.util.Objects;
import rs.teslaris.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class AssessmentMeasureConverter {

    public static AssessmentMeasureDTO toDTO(AssessmentMeasure assessmentMeasure) {
        return new AssessmentMeasureDTO(
            assessmentMeasure.getId(),
            assessmentMeasure.getCode(),
            assessmentMeasure.getPointRule(),
            assessmentMeasure.getScalingRule(),
            MultilingualContentConverter.getMultilingualContentDTO(assessmentMeasure.getTitle()),
            Objects.nonNull(assessmentMeasure.getRulebook()) ?
                assessmentMeasure.getRulebook().getId() : null);
    }
}
