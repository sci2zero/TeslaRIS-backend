package rs.teslaris.assessment.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.CommissionRelationDTO;
import rs.teslaris.assessment.dto.CommissionRelationResponseDTO;
import rs.teslaris.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.core.model.institution.CommissionRelation;
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
