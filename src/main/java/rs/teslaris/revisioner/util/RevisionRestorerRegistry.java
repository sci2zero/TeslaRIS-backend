package rs.teslaris.revisioner.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
public class RevisionRestorerRegistry {

    private final Map<String, RevisionRestorer<?>> restorers;


    public RevisionRestorerRegistry(List<RevisionRestorer<?>> restorerList) {
        this.restorers =
            restorerList.stream()
                .collect(Collectors.toMap(RevisionRestorer::entityType, Function.identity()));
    }

    public Optional<RevisionRestorer<?>> get(String entityType) {
        return Optional.ofNullable(restorers.get(entityType));
    }
}
