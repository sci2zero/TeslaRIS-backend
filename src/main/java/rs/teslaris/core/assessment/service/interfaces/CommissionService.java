package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.CommissionDTO;
import rs.teslaris.core.assessment.dto.CommissionResponseDTO;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CommissionService extends JPAService<Commission> {

    Page<CommissionResponseDTO> readAllCommissions(Pageable pageable,
                                                   String searchExpression,
                                                   String language,
                                                   Boolean selectOnlyLoadCommissions,
                                                   Boolean selectOnlyClassificationCommissions);

    CommissionResponseDTO readCommissionById(Integer commissionId);

    Commission createCommission(CommissionDTO commissionDTO);

    List<String> readAllApplicableRuleEngines();

    void updateCommission(Integer commissionId, CommissionDTO commissionDTO);

    void deleteCommission(Integer commissionId);
}
