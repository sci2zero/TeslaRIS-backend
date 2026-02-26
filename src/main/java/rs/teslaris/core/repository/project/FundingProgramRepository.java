package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.FundingProgram;

@Repository
public interface FundingProgramRepository extends JpaRepository<FundingProgram, Integer> {

    @Query("SELECT COUNT(fc) > 0 FROM FundingCall fc " +
        "WHERE fc.fundingProgram.id = :fundingProgramId")
    boolean hasFundingCalls(Integer fundingProgramId);
}
