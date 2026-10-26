package rs.teslaris.revisioner.util.fixators;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.core.revision.DataQualityFixator;

/**
 * Every fixator bean on the classpath, keyed by {@link DataQualityFixator#key()}.
 */
@Component
public class DataQualityFixatorRegistry {

    private final Map<String, DataQualityFixator> fixators;


    public DataQualityFixatorRegistry(List<DataQualityFixator> fixatorList) {
        this.fixators = fixatorList.stream()
            .sorted(Comparator.comparing(DataQualityFixator::key))
            .collect(
                Collectors.toMap(
                    DataQualityFixator::key, Function.identity(),
                    (first, second) -> first, LinkedHashMap::new
                )
            );
    }

    public List<DataQualityFixator> listFixators() {
        return List.copyOf(fixators.values());
    }

    public Optional<DataQualityFixator> get(String key) {
        return Optional.ofNullable(fixators.get(key));
    }

    public boolean exists(String key) {
        return fixators.containsKey(key);
    }
}
