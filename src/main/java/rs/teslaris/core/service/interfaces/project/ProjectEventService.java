package rs.teslaris.core.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.project.ProjectEvent;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface ProjectEventService extends JPAService<ProjectEvent> {
}
