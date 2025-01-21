package rs.teslaris.core.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.CommissionRelationDTO;
import rs.teslaris.core.assessment.dto.CommissionRelationResponseDTO;
import rs.teslaris.core.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.core.assessment.model.CommissionRelation;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface CommissionRelationService extends JPAService<CommissionRelation> {

    List<CommissionRelationResponseDTO> fetchCommissionRelations(Integer sourceCommissionId);

    void addCommissionRelation(CommissionRelationDTO commissionRelationDTO);

    void updateCommissionRelation(Integer commissionRelationId,
                                  CommissionRelationDTO commissionRelationDTO);

    void deleteCommissionRelation(Integer commissionRelationId);

    public void reorderCommissionRelations(Integer commissionId, Integer relationId,
                                           ReorderCommissionRelationDTO reorderDTO);
}
