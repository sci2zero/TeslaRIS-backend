package rs.teslaris.core.assessment.converter;

import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class EntityAssessmentClassificationConverter {

    public static EntityAssessmentClassificationResponseDTO toDTO(
        EntityAssessmentClassification entityAssessmentClassification) {
        return new EntityAssessmentClassificationResponseDTO(
            MultilingualContentConverter.getMultilingualContentDTO(
                entityAssessmentClassification.getAssessmentClassification().getTitle()),
            entityAssessmentClassification.getTimestamp());
    }
}
