package rs.teslaris.project.migration;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.migration.ProjectMigrationLookup;
import rs.teslaris.project.repository.project.ProjectRepository;

@Service
@RequiredArgsConstructor
public class ProjectMigrationLookupImpl implements ProjectMigrationLookup {

    private final ProjectRepository projectRepository;


    @Override
    @Transactional(readOnly = true)
    public List<Integer> findIdsByNationalId(String nationalId) {
        return projectRepository.findIdsByNationalIdIgnoreCase(nationalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> findIdsByInternalIdentifier(String identifier) {
        return projectRepository.findIdsByInternalIdentifier(identifier);
    }
}
