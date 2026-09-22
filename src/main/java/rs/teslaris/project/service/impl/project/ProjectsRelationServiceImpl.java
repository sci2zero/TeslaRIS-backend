package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectsRelation;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.repository.project.ProjectsRelationRepository;
import rs.teslaris.project.service.interfaces.project.ProjectsRelationService;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectsRelationServiceImpl extends JPAServiceImpl<ProjectsRelation> implements ProjectsRelationService {

    private final ProjectsRelationRepository projectsRelationRepository;

    private final ProjectRepository projectRepository;

    private final MultilingualContentService multilingualContentService;

    @Override
    protected JpaRepository<ProjectsRelation, Integer> getEntityRepository() {
        return projectsRelationRepository;
    }

    @Override
    public ProjectsRelation createRelation(ProjectsRelationDTO dto, Project project) {
        var relation = new ProjectsRelation();

        relation.setRelationType(dto.getRelationType());
        relation.setDateFrom(dto.getDateFrom());
        relation.setDateTo(dto.getDateTo());
        relation.setSourceProjectDescription(
                multilingualContentService.getMultilingualContent(
                        dto.getSourceProjectDescription()));
        relation.setTargetProjectDescription(
                multilingualContentService.getMultilingualContent(
                        dto.getTargetProjectDescription()));

        relation.setSourceProject(project);
        if (Objects.nonNull(dto.getTargetProjectId())) {
            relation.setTargetProject(projectRepository.findById(dto.getTargetProjectId())
                    .orElseThrow(() -> new NotFoundException("Target project does not exist.")));
        }

        // Adds saved relation entity with id != null (if this part is omitted the Set will treat
        // each entity with null value id as the same one, thus overwriting/ignoring it each time)
        return projectsRelationRepository.save(relation);
    }
}
