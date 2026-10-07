package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Component
@RequiredArgsConstructor
public class ProjectEntityCreator implements EntityCreator<ProjectMigrationDTO> {

    private final ProjectService projectService;


    @Override
    public Integer create(ProjectMigrationDTO dto, boolean performIndex) {
        if (Objects.nonNull(dto.rejection())) {
            throw new InvalidSourceValueException(dto.rejection());
        }

        var created = projectService.createProject(dto.project());

        return Objects.isNull(created) ? null : created.getId();
    }
}
