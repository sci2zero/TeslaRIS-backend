package rs.teslaris.project.repository.project;

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
}
