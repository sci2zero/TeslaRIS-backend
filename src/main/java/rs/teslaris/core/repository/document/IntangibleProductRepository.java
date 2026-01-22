package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.IntangibleProduct;

@Repository
public interface IntangibleProductRepository extends JpaRepository<IntangibleProduct, Integer> {

    @Query(value = "SELECT * FROM intangible_products s WHERE " +
        "(:allTime = TRUE OR s.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "s.approve_status = 1 ORDER BY s.id", nativeQuery = true)
    Page<IntangibleProduct> findAllModified(Pageable pageable, boolean allTime);
}
