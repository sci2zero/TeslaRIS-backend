package rs.teslaris.core.service.interfaces.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.assessment.CommissionDTO;
import rs.teslaris.core.model.assessment.Commission;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CommissionService extends JPAService<Commission> {

    Page<CommissionDTO> readAllCommissions(Pageable pageable);

    CommissionDTO readCommissionById(Integer commissionId);

    Commission createCommission(CommissionDTO commissionDTO);

    void updateCommission(Integer commissionId, CommissionDTO commissionDTO);

    void deleteCommission(Integer commissionId);
}
