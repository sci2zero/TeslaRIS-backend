package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.FundingProgram;

@Repository
public interface FundingProgramRepository extends JpaRepository<FundingProgram, Integer> {

    @Query("SELECT COUNT(fc) > 0 FROM FundingCall fc " +
        "WHERE fc.fundingProgram.id = :fundingProgramId")
    boolean hasFundingCalls(Integer fundingProgramId);
}
