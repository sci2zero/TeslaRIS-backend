package rs.teslaris.core.revision;

import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code List<QualityScopeResolver>} satisfiable in a deployment where no module contributes
 * a resolver, so the data quality indexer still starts. It answers for no entity type and is never
 * selected - an entity type without a real resolver falls back to an empty scope either way.
 */
@Component
public class NoOpQualityScopeResolver implements QualityScopeResolver {

    @Override
    public String entityType() {
        return UnmatchedEntityType.VALUE;
    }

    @Override
    public Optional<QualityScope> resolve(Integer entityId) {
        return Optional.empty();
    }
}
