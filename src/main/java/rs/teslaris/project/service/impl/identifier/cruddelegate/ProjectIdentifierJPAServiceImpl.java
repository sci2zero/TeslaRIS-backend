package rs.teslaris.project.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;

@Component
public class ProjectIdentifierJPAServiceImpl extends JPAServiceImpl<ProjectIdentifier> {

    private final ProjectIdentifierRepository projectIdentifierRepository;


    @Autowired
    public ProjectIdentifierJPAServiceImpl(
        ProjectIdentifierRepository projectIdentifierRepository) {
        this.projectIdentifierRepository = projectIdentifierRepository;
    }

    @Override
    protected JpaRepository<ProjectIdentifier, Integer> getEntityRepository() {
        return projectIdentifierRepository;
    }
}
