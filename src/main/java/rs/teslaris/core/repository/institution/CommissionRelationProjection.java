package rs.teslaris.core.repository.institution;

import java.util.List;
import rs.teslaris.core.model.institution.ResultCalculationMethod;

public record CommissionRelationProjection(
    Integer relationId,
    Integer priority,
    List<Integer> targetCommissionIds,
    ResultCalculationMethod resultCalculationMethod
) {
    public static CommissionRelationProjection fromNative(
        Integer relationId,
        Integer priority,
        Integer[] targetCommissionIds,
        String resultCalculationMethod
    ) {
        return new CommissionRelationProjection(
            relationId,
            priority,
            targetCommissionIds == null ? List.of() : List.of(targetCommissionIds),
            ResultCalculationMethod.valueOf(resultCalculationMethod)
        );
    }
}


