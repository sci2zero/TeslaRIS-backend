package rs.teslaris.project.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
}
