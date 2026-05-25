package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.model.project.ProjectEvent;

import java.util.concurrent.CompletableFuture;

@Service
public interface ProjectEventService extends JPAService<ProjectEvent> {

    ProjectEvent createProjectEvent(ProjectEventDTO projectEventDTO);

    void deleteProjectEvent(Integer projectEventId);

}
