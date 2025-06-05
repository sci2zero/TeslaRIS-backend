package rs.teslaris.assessment.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.CommissionRelationConverter;
import rs.teslaris.assessment.dto.CommissionRelationDTO;
import rs.teslaris.assessment.dto.CommissionRelationResponseDTO;
import rs.teslaris.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.assessment.repository.CommissionRelationRepository;
import rs.teslaris.assessment.service.interfaces.CommissionRelationService;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.institution.CommissionRelation;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class CommissionRelationServiceImpl extends JPAServiceImpl<CommissionRelation>
    implements CommissionRelationService {

    private final CommissionRelationRepository commissionRelationRepository;

    private final CommissionService commissionService;


    @Override
    public List<CommissionRelationResponseDTO> fetchCommissionRelations(
        Integer sourceCommissionId) {
        return commissionRelationRepository.getRelationsForSourceCommission(sourceCommissionId)
            .stream().sorted(Comparator.comparingInt(CommissionRelation::getPriority)).map(
                CommissionRelationConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public void addCommissionRelation(CommissionRelationDTO commissionRelationDTO) {
        var newRelation = new CommissionRelation();

        setCommonFields(newRelation, commissionRelationDTO);
        newRelation.getSourceCommission().getRelations().add(newRelation);

        save(newRelation);
    }

    @Override
    public void updateCommissionRelation(Integer commissionRelationId,
                                         CommissionRelationDTO commissionRelationDTO) {
        var commissionRelationToUpdate = findOne(commissionRelationId);

        setCommonFields(commissionRelationToUpdate, commissionRelationDTO);

        save(commissionRelationToUpdate);
    }

    private void setCommonFields(CommissionRelation relation, CommissionRelationDTO dto) {
        relation.setSourceCommission(commissionService.findOne(
            dto.getSourceCommissionId()));
        relation.setPriority(dto.getPriority());
        relation.setResultCalculationMethod(dto.getResultCalculationMethod());

        relation.getTargetCommissions().clear();
        dto.getTargetCommissionIds().forEach((targetCommissionId) -> {
            relation.getTargetCommissions().add(commissionService.findOne(targetCommissionId));
        });
    }

    @Override
    public void deleteCommissionRelation(Integer commissionRelationId) {
        var commissionRelationToDelete = findOne(commissionRelationId);

        commissionRelationToDelete.getSourceCommission().getRelations()
            .remove(commissionRelationToDelete);
        commissionService.save(commissionRelationToDelete.getSourceCommission());

        delete(commissionRelationId);
    }

    @Override
    protected JpaRepository<CommissionRelation, Integer> getEntityRepository() {
        return commissionRelationRepository;
    }

    @Override
    public void reorderCommissionRelations(Integer commissionId, Integer relationId,
                                           ReorderCommissionRelationDTO reorderDTO) {
        var commissionToUpdate = commissionService.findOne(commissionId);
        reorderRelations(commissionToUpdate.getRelations(), relationId,
            reorderDTO.getOldRelationPriority(),
            reorderDTO.getNewRelationPriority());

        commissionService.save(commissionToUpdate);
    }

    private void reorderRelations(Set<CommissionRelation> relations,
                                  Integer relationId,
                                  Integer oldRelationPriority,
                                  Integer newRelationPriority) {
        if (oldRelationPriority > newRelationPriority) {
            relations.forEach(relation -> {
                if (relation.getId().equals(relationId)) {
                    relation.setPriority(newRelationPriority);
                } else if (relation.getPriority() >= newRelationPriority &&
                    relation.getPriority() < oldRelationPriority) {
                    relation.setPriority(relation.getPriority() + 1);
                }
            });
        } else if (oldRelationPriority < newRelationPriority) {
            relations.forEach(relation -> {
                if (relation.getId().equals(relationId)) {
                    relation.setPriority(newRelationPriority);
                } else if (relation.getPriority() > oldRelationPriority &&
                    relation.getPriority() <= newRelationPriority) {
                    relation.setPriority(relation.getPriority() - 1);
                }
            });
        }
    }
}
