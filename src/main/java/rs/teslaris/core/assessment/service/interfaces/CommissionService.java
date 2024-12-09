package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.CommissionDTO;
import rs.teslaris.core.assessment.dto.CommissionResponseDTO;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CommissionService extends JPAService<Commission> {

    Page<CommissionResponseDTO> readAllCommissions(Pageable pageable, String searchExpression);

    CommissionResponseDTO readCommissionById(Integer commissionId);

    Commission createCommission(CommissionDTO commissionDTO);

    void updateCommission(Integer commissionId, CommissionDTO commissionDTO);

    void deleteCommission(Integer commissionId);
}
