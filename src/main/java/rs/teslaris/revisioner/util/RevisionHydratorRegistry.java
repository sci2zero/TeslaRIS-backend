package rs.teslaris.revisioner.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class RevisionHydratorRegistry {

    private final Map<String, RevisionHydrator<?>> hydrators;

    private final Map<String, Class<?>> dtoClasses = Map.of(
        "DATASET", DatasetDTO.class,
        "INTANGIBLE_PRODUCT", IntangibleProductDTO.class
    );


    public RevisionHydratorRegistry(List<RevisionHydrator<?>> hydratorList) {
        this.hydrators =
            hydratorList.stream()
                .collect(Collectors.toMap(RevisionHydrator::entityType, Function.identity()));
    }

    public Optional<RevisionHydrator<?>> get(String entityType) {
        return Optional.ofNullable(hydrators.get(entityType));
    }

    public Class<?> getDtoClass(String entityType) {
        return dtoClasses.get(entityType);
    }
}
