package rs.teslaris.project.revision;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.QualityScope;
import rs.teslaris.core.revision.QualityScopeResolver;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;

/**
 * A project's team and consortium are excluded from its revision snapshot - {@code updateProject}
 * never writes them - so the scope is read from the project index, which already carries both the
 * contributing persons and their institutions expanded over the super hierarchy.
 */
@Component
@RequiredArgsConstructor
public class ProjectQualityScopeResolver implements QualityScopeResolver {

    private final ProjectIndexRepository projectIndexRepository;


    @Override
    public String entityType() {
        return EntityType.PROJECT.name();
    }

    @Override
    public Optional<QualityScope> resolve(Integer entityId) {
        return projectIndexRepository.findProjectIndexByDatabaseId(entityId)
            .map(index -> new QualityScope(
                index.getPersonIds(),
                index.getOrganisationUnitIds(),
                index.getNameSr(),
                index.getNameOther()
            ));
    }
}
