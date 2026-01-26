package rs.teslaris.assessment.repository.classification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.classification.PrizeAssessmentClassification;

@Repository
public interface PrizeAssessmentClassificationRepository
    extends JpaRepository<PrizeAssessmentClassification, Integer> {

    @Query("SELECT pac FROM PrizeAssessmentClassification pac WHERE " +
        "pac.prize.id = :prizeId ORDER BY pac.timestamp DESC")
    List<PrizeAssessmentClassification> findAssessmentClassificationsForPrize(
        Integer prizeId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PrizeAssessmentClassification pac " +
        "WHERE pac.prize.id = :prizeId " +
        "AND pac.commission.id = :commissionId " +
        "AND pac.manual = :isManual")
    void deleteByPrizeIdAndCommissionId(Integer prizeId, Integer commissionId, Boolean isManual);
}
