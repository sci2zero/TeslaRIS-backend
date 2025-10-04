package rs.teslaris.core.util.functional;

@FunctionalInterface
public interface BiPredicate<T, U> {
    boolean test(T t, U u);
}
