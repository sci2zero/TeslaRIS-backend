package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.FundingPart;

@Repository
public interface FundingPartRepository extends JpaRepository<FundingPart, Integer> {

    @Query("SELECT COUNT(fp) > 0 FROM FundingPart fp WHERE fp.amount.currency.id = :currencyId")
    boolean isCurrencyInUse(Integer currencyId);
}
