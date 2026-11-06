package rs.teslaris.project.repository.funding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.Funding;

@Repository
public interface FundingRepository extends JpaRepository<Funding, Integer> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Funding f WHERE f.doi = :doi AND (:id IS NULL OR f.id <> :id)")
    boolean existsByDoi(String doi, Integer id);

    @Query("SELECT COUNT(f) > 0 FROM Funding f WHERE f.amount.currency.id = :currencyId")
    boolean isCurrencyInUse(Integer currencyId);

    String MODIFIED_FILTER =
        "WHERE (:allTime = TRUE " +
            "OR f.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY' " +
            "OR EXISTS (SELECT 1 FROM projects p WHERE p.id = f.project_id " +
            "AND p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY')) " +
            "AND (f.date_awarded IS NOT NULL OR f.funding_types IS NULL " +
            "OR NOT f.funding_types @> '[\"CALL\"]') ";

    @Query(value = "SELECT f.* FROM fundings f " + MODIFIED_FILTER + "ORDER BY f.id",
        countQuery = "SELECT COUNT(*) FROM fundings f " + MODIFIED_FILTER,
        nativeQuery = true)
    Page<Funding> findAllModified(Pageable pageable, boolean allTime);
}
