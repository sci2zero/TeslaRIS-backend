package rs.teslaris.assessment.converter;

import rs.teslaris.assessment.dto.CommissionResponseDTO;
import rs.teslaris.assessment.model.Commission;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class CommissionConverter {

    public static CommissionResponseDTO toDTO(Commission commission) {
        return new CommissionResponseDTO(commission.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(commission.getDescription()),
            commission.getSources().stream().toList(), commission.getAssessmentDateFrom(),
            commission.getAssessmentDateTo(),
            commission.getFormalDescriptionOfRule(),
            commission.getRecognisedResearchAreas().stream().toList());
    }
}
