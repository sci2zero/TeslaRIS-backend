package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.GeneticMaterial;

@Repository
public interface GeneticMaterialRepository extends JpaRepository<GeneticMaterial, Integer> {

    @Query(value = "SELECT * FROM genetic_materials s WHERE " +
        "(:allTime = TRUE OR s.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') AND " +
        "s.approve_status = 1 ORDER BY s.id", nativeQuery = true)
    Page<GeneticMaterial> findAllModified(Pageable pageable, boolean allTime);
}
