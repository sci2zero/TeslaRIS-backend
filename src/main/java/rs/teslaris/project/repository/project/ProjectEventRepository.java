package rs.teslaris.project.repository.project;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.project.ProjectEvent;

@Repository
public interface ProjectEventRepository extends JpaRepository<ProjectEvent, Integer> {

    @Query("SELECT pe.event.id FROM ProjectEvent pe WHERE pe.project.id = :projectId")
    List<Integer> findEventIdsByProjectId(Integer projectId);

}
