package rs.teslaris.thesislibrary.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.thesislibrary.model.RegistryBookReport;

@Repository
public interface RegistryBookReportRepository extends JpaRepository<RegistryBookReport, Integer> {

    Optional<RegistryBookReport> findByReportFileName(String reportFileName);

    @Query("SELECT rbr FROM RegistryBookReport rbr WHERE rbr.institution.id = :institutionId")
    List<RegistryBookReport> findForInstitution(Integer institutionId);

    @Query("SELECT rbr FROM RegistryBookReport rbr WHERE rbr.createDate <= :startDate")
    List<RegistryBookReport> findStaleReports(Date startDate);
}
