package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.Project;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface ProjectRepository extends JPASoftDeleteRepository<Project> {
}
