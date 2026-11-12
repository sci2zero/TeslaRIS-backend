package rs.teslaris.exporter.util.push;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import rs.teslaris.exporter.model.common.BaseExportEntity;

@Component
public class RdfMapperRegistry {

    private final Map<String, RdfMapper<?>> mappers = new HashMap<>();


    public RdfMapperRegistry(List<RdfMapper<?>> mappers) {
        mappers.forEach(mapper -> {
            var key = key(mapper.profile(), mapper.sourceType());
            if (this.mappers.putIfAbsent(key, mapper) != null) {
                throw new IllegalStateException("More than one RDF mapper for " + key);
            }
        });
    }

    private static String key(String profile, Class<?> sourceType) {
        return profile.toLowerCase(Locale.ROOT) + "|" + sourceType.getName();
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseExportEntity> Optional<RdfMapper<T>> find(String profile,
                                                                   Class<T> sourceType) {
        return Optional.ofNullable((RdfMapper<T>) mappers.get(key(profile, sourceType)));
    }
}
