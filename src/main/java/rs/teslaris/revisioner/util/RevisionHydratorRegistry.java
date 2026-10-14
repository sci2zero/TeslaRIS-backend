package rs.teslaris.revisioner.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.core.revision.RevisionRestorer;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class RevisionHydratorRegistry {

    private final Map<String, RevisionHydrator<?>> hydrators;

    private final Map<String, Class<?>> dtoClasses;

    private final Map<String, Class<?>> assessmentDtoClasses;


    public RevisionHydratorRegistry(List<RevisionHydrator<?>> hydratorList,
                                    List<RevisionRestorer<?>> restorerList) {
        this.hydrators =
            hydratorList.stream()
                .collect(Collectors.toMap(RevisionHydrator::entityType, Function.identity()));

        // Every entity type that can be restored can also be read back, and the restorer already
        // declares which class to deserialize into - a second hand-maintained map only drifts.
        this.dtoClasses =
            restorerList.stream()
                .collect(Collectors.toMap(RevisionRestorer::entityType,
                    RevisionRestorer::readDtoClass));

        this.assessmentDtoClasses =
            restorerList.stream()
                .collect(Collectors.toMap(RevisionRestorer::entityType,
                    RevisionRestorer::assessmentDtoClass));
    }

    public Optional<RevisionHydrator<?>> get(String entityType) {
        return Optional.ofNullable(hydrators.get(entityType));
    }

    public Class<?> getDtoClass(String entityType) {
        return dtoClasses.get(entityType);
    }

    /**
     * The class the calculator assesses a revision as. Differs from {@link #getDtoClass(String)}
     * only for entity types whose snapshot DTO the calculator may not import.
     */
    public Class<?> getAssessmentDtoClass(String entityType) {
        return assessmentDtoClasses.get(entityType);
    }
}
