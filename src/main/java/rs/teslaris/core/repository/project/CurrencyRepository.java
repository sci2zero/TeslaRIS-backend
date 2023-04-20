package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
}
