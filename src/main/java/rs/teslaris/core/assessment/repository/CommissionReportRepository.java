package rs.teslaris.core.assessment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.assessment.model.CommissionReport;

@Repository
public interface CommissionReportRepository extends JpaRepository<CommissionReport, Integer> {

    @Query("SELECT cr.reportFileName FROM CommissionReport cr " +
        "WHERE cr.commission.id = :commissionId")
    List<String> getAvailableReportsForCommission(Integer commissionId);

    @Query("SELECT cr FROM CommissionReport cr " +
        "WHERE cr.commission.id = :commissionId AND cr.reportFileName = :reportFileName")
    Optional<CommissionReport> getReport(Integer commissionId, String reportFileName);

    @Query("SELECT count(cr) > 0 FROM CommissionReport cr " +
        "WHERE cr.commission.id = :commissionId AND cr.reportFileName = :reportFileName")
    boolean reportExists(Integer commissionId, String reportFileName);
}
