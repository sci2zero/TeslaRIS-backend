package rs.teslaris.core.harvester.utility;

@FunctionalInterface
public interface CreatorMethod<D, R> {
    R apply(D dto, boolean performIndex);
}