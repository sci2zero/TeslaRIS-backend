package rs.teslaris.assessment.converter;

import rs.teslaris.assessment.dto.CommissionResponseDTO;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.model.institution.Commission;

public class CommissionConverter {

    public static CommissionResponseDTO toDTO(Commission commission) {
        return new CommissionResponseDTO(commission.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(commission.getDescription()),
            commission.getSources().stream().toList(), commission.getAssessmentDateFrom(),
            commission.getAssessmentDateTo(),
            commission.getFormalDescriptionOfRule(),
            commission.getRecognisedResearchAreas().stream().toList(),
            commission.getIsDefault());
    }
}
