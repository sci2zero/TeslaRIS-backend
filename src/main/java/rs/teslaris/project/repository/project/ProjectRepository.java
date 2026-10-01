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
}
