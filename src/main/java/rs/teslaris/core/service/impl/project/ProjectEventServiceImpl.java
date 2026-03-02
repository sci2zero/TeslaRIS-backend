package rs.teslaris.core.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.project.ProjectEvent;
import rs.teslaris.core.repository.project.ProjectEventRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.project.ProjectEventService;

@Service
@RequiredArgsConstructor
public class ProjectEventServiceImpl extends JPAServiceImpl<ProjectEvent>
    implements ProjectEventService {

    private final ProjectEventRepository projectEventRepository;

    @Override
    protected JpaRepository<ProjectEvent, Integer> getEntityRepository() {
        return projectEventRepository;
    }
}
