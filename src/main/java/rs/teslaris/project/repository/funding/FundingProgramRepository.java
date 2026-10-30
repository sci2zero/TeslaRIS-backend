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

    @Query("SELECT COUNT(fp) > 0 FROM FundingProgram fp " +
        "WHERE fp.totalAmount.currency.id = :currencyId")
    boolean isCurrencyInUse(Integer currencyId);
}
