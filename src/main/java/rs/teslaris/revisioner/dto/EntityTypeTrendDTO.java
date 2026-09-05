package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;

public record EntityTypeTrendDTO(

    RepositoryEntityType entityType,

    @Nullable
    Double current,

    @Nullable
    Double previous,

    @Nullable
    Double change,

    boolean supported
) {
    public static EntityTypeTrendDTO unsupported(RepositoryEntityType entityType) {
        return new EntityTypeTrendDTO(entityType, null, null, null, false);
    }
}
