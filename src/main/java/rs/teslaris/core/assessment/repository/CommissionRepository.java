package rs.teslaris.core.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.Commission;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    @Query("select count(eac) > 0 from EntityAssessmentClassification eac where eac.commission.id = :commissionId")
    boolean isInUse(Integer commissionId);
}
