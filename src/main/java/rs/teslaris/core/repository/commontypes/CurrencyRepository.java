package rs.teslaris.core.repository.commontypes;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {

    Optional<Currency> findByCode(String code);

    @Query(value =
        "SELECT c FROM Currency c LEFT JOIN c.name name " +
            "WHERE LOWER(c.code) = :searchExpression OR " +
            "LOWER(c.symbol) = :searchExpression OR " +
            "(name.language.languageTag = :languageTag AND " +
            "(LOWER(name.content) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
            "c.processedName LIKE CONCAT('%', :searchExpression, '%')))",
        countQuery =
            "SELECT count(DISTINCT c) FROM Currency c JOIN c.name n " +
                "WHERE LOWER(c.code) = :searchExpression OR " +
                "LOWER(c.symbol) = :searchExpression OR " +
                "(n.language.languageTag = :languageTag AND " +
                "(LOWER(n.content) LIKE LOWER(CONCAT('%', :searchExpression, '%')) OR " +
                "c.processedName LIKE CONCAT('%', :searchExpression, '%')))")
    Page<Currency> searchCurrencies(String searchExpression, String languageTag,
                                    Pageable pageable);
}
