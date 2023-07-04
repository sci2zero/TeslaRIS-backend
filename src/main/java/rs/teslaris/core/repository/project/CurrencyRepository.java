package rs.teslaris.core.repository.project;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.Currency;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface CurrencyRepository extends JPASoftDeleteRepository<Currency> {
}
