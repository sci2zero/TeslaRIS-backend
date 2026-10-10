package rs.teslaris.project.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.QualityIdentifierField;
import rs.teslaris.core.revision.QualityUniquenessChecker;
import rs.teslaris.project.repository.project.ProjectRepository;

@Component
@RequiredArgsConstructor
public class ProjectQualityUniquenessChecker implements QualityUniquenessChecker {

    private final ProjectRepository projectRepository;


    @Override
    public String entityType() {
        return EntityType.PROJECT.name();
    }

    @Override
    public boolean isDuplicate(QualityIdentifierField field, String value, Integer entityId) {
        return switch (field) {
            case DOI -> projectRepository.existsByDoi(value, entityId);
            case RAID -> projectRepository.existsByRaid(value, entityId);
            case NATIONAL_ID -> projectRepository.existsByNationalId(value, entityId);
        };
    }
}
