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
            entityAssessmentClassification.getAssessmentClassification().getId(),
            Objects.nonNull(entityAssessmentClassification.getCommission()) ?
                MultilingualContentConverter.getMultilingualContentDTO(
                    entityAssessmentClassification.getCommission().getDescription()) :
                new ArrayList<>(),
            Objects.nonNull(entityAssessmentClassification.getCommission()) ?
                entityAssessmentClassification.getCommission().getId() : null,
            entityAssessmentClassification.getCategoryIdentifier(),
            entityAssessmentClassification.getClassificationYear(),
            entityAssessmentClassification.getTimestamp(),
            entityAssessmentClassification.getAssessmentClassification().getApplicableTypes()
                .stream().toList(), entityAssessmentClassification.getManual(),
            MultilingualContentConverter.getMultilingualContentDTO(
                entityAssessmentClassification.getClassificationReason()));
    }

    public static EntityAssessmentClassificationResponseDTO toDTO(
        EntityAssessmentClassification entityAssessmentClassification) {
        return new EntityAssessmentClassificationResponseDTO(
            entityAssessmentClassification.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(
                entityAssessmentClassification.getAssessmentClassification().getTitle()),
            entityAssessmentClassification.getAssessmentClassification().getId(),
            Objects.nonNull(entityAssessmentClassification.getCommission()) ?
                MultilingualContentConverter.getMultilingualContentDTO(
                    entityAssessmentClassification.getCommission().getDescription()) :
                new ArrayList<>(),
            Objects.nonNull(entityAssessmentClassification.getCommission()) ?
                entityAssessmentClassification.getCommission().getId() : null,
            "",
            entityAssessmentClassification.getClassificationYear(),
            entityAssessmentClassification.getTimestamp(),
            entityAssessmentClassification.getAssessmentClassification().getApplicableTypes()
                .stream().toList(), entityAssessmentClassification.getManual(),
            MultilingualContentConverter.getMultilingualContentDTO(
                entityAssessmentClassification.getClassificationReason()));
    }
}
