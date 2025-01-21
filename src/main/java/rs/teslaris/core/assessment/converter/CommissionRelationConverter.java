package rs.teslaris.core.assessment.converter;

import java.util.ArrayList;
import rs.teslaris.core.assessment.dto.CommissionRelationResponseDTO;
import rs.teslaris.core.assessment.dto.SimpleCommissionResponseDTO;
import rs.teslaris.core.assessment.model.CommissionRelation;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;

public class CommissionRelationConverter {

    public static CommissionRelationResponseDTO toDTO(CommissionRelation commissionRelation) {
        var dto = new CommissionRelationResponseDTO();
        dto.setId(commissionRelation.getId());
        dto.setPriority(commissionRelation.getPriority());
        dto.setResultCalculationMethod(commissionRelation.getResultCalculationMethod());

        dto.setSourceCommissionId(commissionRelation.getSourceCommission().getId());

        dto.setTargetCommissions(new ArrayList<>());
        commissionRelation.getTargetCommissions().forEach(targetCommission -> {
            dto.getTargetCommissions().add(new SimpleCommissionResponseDTO(targetCommission.getId(),
                MultilingualContentConverter.getMultilingualContentDTO(
                    targetCommission.getDescription())));
        });

        return dto;
    }
}
