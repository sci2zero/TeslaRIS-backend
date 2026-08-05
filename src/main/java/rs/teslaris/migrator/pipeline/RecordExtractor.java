package rs.teslaris.migrator.pipeline;

import java.util.List;
import java.util.Objects;

/**
 * Projects one source record onto zero or more DTOs of a single type.
 * <p>
 * A source record is not necessarily one entity: a hydrator curriculum yields n institutions, one
 * person and n employments, while a project record yields exactly one project. Sources of the
 * latter kind implement {@link RecordConverter} and use {@link #of(RecordConverter)} instead of
 * writing list boilerplate.
 */
public interface RecordExtractor<S, D> {

    static <S, D> RecordExtractor<S, D> of(RecordConverter<S, D> converter) {
        return record -> {
            var dto = converter.toDTO(record);
            return Objects.isNull(dto) ? List.of() : List.of(dto);
        };
    }

    List<D> extract(S record);

    /**
     * 1:1 conversion. Returning {@code null} means "skip this record", matching the convention used
     * by the older OAI-PMH migration converters.
     */
    @FunctionalInterface
    interface RecordConverter<S, D> {
        D toDTO(S record);
    }
}
