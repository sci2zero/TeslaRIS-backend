package rs.teslaris.core.repository.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.assessment.Commission;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    @Query("select count(eac) > 0 from EntityAssessmentClassification eac where eac.commission.id = :commissionId")
    boolean isInUse(Integer commissionId);
}
