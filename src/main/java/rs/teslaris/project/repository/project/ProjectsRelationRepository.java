package rs.teslaris.project.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.ProjectsRelation;

@Repository
public interface ProjectsRelationRepository extends JpaRepository<ProjectsRelation, Integer> {
}
