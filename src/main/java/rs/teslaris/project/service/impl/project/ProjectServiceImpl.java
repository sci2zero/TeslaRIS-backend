package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends JPAServiceImpl<Project> implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    protected JpaRepository<Project, Integer> getEntityRepository() {
        return projectRepository;
    }
}
