package rs.teslaris.core.assessment.converter;

import java.util.ArrayList;
import java.util.Objects;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class EntityAssessmentClassificationConverter {

    public static EntityAssessmentClassificationResponseDTO toDTO(
        PublicationSeriesAssessmentClassification entityAssessmentClassification) {
        return new EntityAssessmentClassificationResponseDTO(
            entityAssessmentClassification.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(
                entityAssessmentClassification.getAssessmentClassification().getTitle()),
            Objects.nonNull(entityAssessmentClassification.getCommission()) ?
                MultilingualContentConverter.getMultilingualContentDTO(
                    entityAssessmentClassification.getCommission().getDescription()) :
                new ArrayList<>(),
            entityAssessmentClassification.getCategoryIdentifier(),
            entityAssessmentClassification.getClassificationYear(),
            entityAssessmentClassification.getTimestamp());
    }

    public static EntityAssessmentClassificationResponseDTO toDTO(
        EntityAssessmentClassification entityAssessmentClassification) {
        return new EntityAssessmentClassificationResponseDTO(
            entityAssessmentClassification.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(
                entityAssessmentClassification.getAssessmentClassification().getTitle()),
            Objects.nonNull(entityAssessmentClassification.getCommission()) ?
                MultilingualContentConverter.getMultilingualContentDTO(
                    entityAssessmentClassification.getCommission().getDescription()) :
                new ArrayList<>(),
            "",
            entityAssessmentClassification.getClassificationYear(),
            entityAssessmentClassification.getTimestamp());
    }
}
