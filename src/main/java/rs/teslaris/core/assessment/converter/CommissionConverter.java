package rs.teslaris.core.assessment.converter;

import rs.teslaris.core.assessment.dto.CommissionResponseDTO;
import rs.teslaris.core.assessment.model.Commission;
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
