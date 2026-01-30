package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.dto.CommissionRelationDTO;
import rs.teslaris.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.assessment.repository.CommissionRelationRepository;
import rs.teslaris.assessment.service.impl.CommissionRelationServiceImpl;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.CommissionRelation;
import rs.teslaris.core.model.institution.ResultCalculationMethod;
import rs.teslaris.core.repository.institution.CommissionRepository;

@SpringBootTest
public class CommissionRelationServiceTest {

    @Mock
    private CommissionRelationRepository commissionRelationRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @InjectMocks
    private CommissionRelationServiceImpl commissionRelationService;


    @Test
    void shouldFetchCommissionRelations() {
        // Given
        var sourceCommissionId = 1;
        var relation = new CommissionRelation();
        relation.setSourceCommission(new Commission());
        relation.setTargetCommissions(Set.of(new Commission()));
        var commissionRelations = List.of(relation);
        when(commissionRelationRepository.getRelationsForSourceCommission(sourceCommissionId))
            .thenReturn(commissionRelations);

        // When
        var result = commissionRelationService.fetchCommissionRelations(sourceCommissionId);

        // Then
        verify(commissionRelationRepository).getRelationsForSourceCommission(sourceCommissionId);
        assertEquals(commissionRelations.size(), result.size());
    }

    @Test
    void shouldAddCommissionRelation() {
        // Given
        var commissionRelationDTO =
            new CommissionRelationDTO(1, List.of(2, 3), 10, ResultCalculationMethod.WORST_VALUE);
        var sourceCommission = new Commission();
        when(commissionRepository.findById(1)).thenReturn(Optional.of(sourceCommission));
        when(commissionRepository.findCommissionsByIds(any())).thenReturn(
            List.of(new Commission(), new Commission()));

        // When
        commissionRelationService.addCommissionRelation(commissionRelationDTO);

        // Then
        verify(commissionRepository).getReferenceById(1);
        verify(commissionRelationRepository).save(any(CommissionRelation.class));
    }

    @Test
    void shouldUpdateCommissionRelation() {
        // Given
        var commissionRelationId = 1;
        var commissionRelationDTO =
            new CommissionRelationDTO(1, List.of(2), 5, ResultCalculationMethod.BEST_VALUE);
        var commissionRelation = new CommissionRelation();
        when(commissionRelationRepository.findById(commissionRelationId))
            .thenReturn(Optional.of(commissionRelation));
        when(commissionRepository.findById(1)).thenReturn(Optional.of(new Commission()));
        when(commissionRepository.findCommissionsByIds(any())).thenReturn(
            List.of(new Commission()));

        // When
        commissionRelationService.updateCommissionRelation(commissionRelationId,
            commissionRelationDTO);

        // Then
        verify(commissionRelationRepository).save(commissionRelation);
    }

    @Test
    void shouldDeleteCommissionRelation() {
        // Given
        var commissionRelationId = 1;
        var commissionRelation = new CommissionRelation();
        var sourceCommission = new Commission();
        commissionRelation.setSourceCommission(sourceCommission);
        when(commissionRelationRepository.findById(commissionRelationId))
            .thenReturn(Optional.of(commissionRelation));

        // When
        commissionRelationService.deleteCommissionRelation(commissionRelationId);

        // Then
        verify(commissionRelationRepository).save(any());
        verify(commissionRepository).save(sourceCommission);
    }

    @Test
    void shouldReorderCommissionRelations() {
        // Given
        var commissionId = 1;
        var relationId = 2;
        var reorderDTO = new ReorderCommissionRelationDTO(1, 3);
        var commission = new Commission();
        var relations = new HashSet<>(Set.of(
            createRelation(1, 1),
            createRelation(2, 2),
            createRelation(3, 3)
        ));
        commission.setRelations(relations);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(commission));

        // When
        commissionRelationService.reorderCommissionRelations(commissionId, relationId, reorderDTO);

        // Then
        verify(commissionRepository).save(commission);
    }

    private CommissionRelation createRelation(int id, int priority) {
        var relation = new CommissionRelation();
        relation.setId(id);
        relation.setPriority(priority);
        return relation;
    }
}
