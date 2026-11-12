package rs.teslaris.project.repository.project;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Project p WHERE p.nationalId = :nationalId AND (:id IS NULL OR p.id <> :id)")
    boolean existsByNationalId(String nationalId, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Project p WHERE p.doi = :doi AND (:id IS NULL OR p.id <> :id)")
    boolean existsByDoi(String doi, Integer id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Project p WHERE p.raid = :raid AND (:id IS NULL OR p.id <> :id)")
    boolean existsByRaid(String raid, Integer id);

    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.costs.currency.id = :currencyId")
    boolean isCurrencyInUse(Integer currencyId);

    @Query(value = "SELECT * FROM projects p WHERE " +
        "(:allTime = TRUE OR p.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') " +
        "AND p.publication_candidate IS NOT FALSE ORDER BY p.id", nativeQuery = true)
    Page<Project> findAllModified(Pageable pageable, boolean allTime);

    @Query("SELECT p.id FROM Project p WHERE lower(p.nationalId) = lower(:nationalId) " +
        "ORDER BY p.id")
    List<Integer> findIdsByNationalIdIgnoreCase(String nationalId);

    @Query(value = "SELECT id FROM projects WHERE " +
        "internal_identifiers @> to_jsonb(array[cast(?1 as text)]) AND deleted = FALSE ORDER BY id",
        nativeQuery = true)
    List<Integer> findIdsByInternalIdentifier(String identifier);
}
