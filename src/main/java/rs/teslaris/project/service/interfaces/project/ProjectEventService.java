package rs.teslaris.project.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.model.project.ProjectEvent;

@Service
public interface ProjectEventService extends JPAService<ProjectEvent> {
}
