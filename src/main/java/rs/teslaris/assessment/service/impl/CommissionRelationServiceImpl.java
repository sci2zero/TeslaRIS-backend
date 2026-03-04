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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.institution.CommissionRelation;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Service
@RequiredArgsConstructor
@Traceable
public class CommissionRelationServiceImpl extends JPAServiceImpl<CommissionRelation>
    implements CommissionRelationService {

    private final CommissionRelationRepository commissionRelationRepository;

    private final CommissionRepository commissionRepository;


    @Override
    @Transactional(readOnly = true)
    public List<CommissionRelationResponseDTO> fetchCommissionRelations(
        Integer sourceCommissionId) {
        return commissionRelationRepository.getRelationsForSourceCommission(sourceCommissionId)
            .stream().sorted(Comparator.comparingInt(CommissionRelation::getPriority)).map(
                CommissionRelationConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addCommissionRelation(CommissionRelationDTO commissionRelationDTO) {
        var newRelation = new CommissionRelation();

        setCommonFields(newRelation, commissionRelationDTO);

        save(newRelation);
    }

    @Override
    @Transactional
    public void updateCommissionRelation(Integer commissionRelationId,
                                         CommissionRelationDTO commissionRelationDTO) {
        var commissionRelationToUpdate = findOne(commissionRelationId);

        setCommonFields(commissionRelationToUpdate, commissionRelationDTO);

        save(commissionRelationToUpdate);
    }

    private void setCommonFields(CommissionRelation relation, CommissionRelationDTO dto) {
        relation.setSourceCommission(commissionRepository.getReferenceById(
            dto.getSourceCommissionId()));
        relation.setPriority(dto.getPriority());
        relation.setResultCalculationMethod(dto.getResultCalculationMethod());

        relation.getTargetCommissions().clear();
        relation.getTargetCommissions()
            .addAll(commissionRepository.findCommissionsByIds(dto.getTargetCommissionIds()));
    }

    @Override
    @Transactional
    public void deleteCommissionRelation(Integer commissionRelationId) {
        var commissionRelationToDelete = findOne(commissionRelationId);

        commissionRepository.save(commissionRelationToDelete.getSourceCommission());

        delete(commissionRelationId);
    }

    @Override
    protected JpaRepository<CommissionRelation, Integer> getEntityRepository() {
        return commissionRelationRepository;
    }

    @Override
    @Transactional
    public void reorderCommissionRelations(Integer commissionId, Integer relationId,
                                           ReorderCommissionRelationDTO reorderDTO) {
        commissionRepository.findById(commissionId).ifPresent(commissionToUpdate -> {
            reorderRelations(commissionToUpdate.getRelations(), relationId,
                reorderDTO.getOldRelationPriority(),
                reorderDTO.getNewRelationPriority());

            commissionRepository.save(commissionToUpdate);
        });
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
