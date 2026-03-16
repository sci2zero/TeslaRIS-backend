package rs.teslaris.project.repository.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.common.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
}
