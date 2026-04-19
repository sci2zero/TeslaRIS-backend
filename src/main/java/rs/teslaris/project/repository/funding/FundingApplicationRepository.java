package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.FundingApplication;

@Repository
public interface FundingApplicationRepository extends JpaRepository<FundingApplication, Integer> {

    @Query("SELECT COUNT(fa) > 0 FROM FundingApplication fa WHERE fa.revisedFundingApplication.id = :id")
    boolean isRevisedByOther(@Param("id") Integer id);

    @Query("SELECT COUNT(fa) > 0 FROM FundingApplication fa WHERE fa.funding IS NOT NULL AND fa.id = :id")
    boolean hasFunding(@Param("id") Integer id);
}
