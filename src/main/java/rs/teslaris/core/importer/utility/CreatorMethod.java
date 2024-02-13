package rs.teslaris.core.importer.utility;

@FunctionalInterface
public interface CreatorMethod<D, R> {
    R apply(D dto, boolean performIndex);
}