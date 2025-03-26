package rs.teslaris.importer.utility;

@FunctionalInterface
public interface CreatorMethod<D, R> {
    R apply(D dto, boolean performIndex);
}