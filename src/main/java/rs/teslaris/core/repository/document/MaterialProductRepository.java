package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.MaterialProduct;

@Repository
public interface MaterialProductRepository extends JpaRepository<MaterialProduct, Integer> {

    @Query(value = "SELECT * FROM material_product s WHERE " +
        "(:allTime = TRUE OR s.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "s.approve_status = 1 ORDER BY s.id", nativeQuery = true)
    Page<MaterialProduct> findAllModified(Pageable pageable, boolean allTime);
}
