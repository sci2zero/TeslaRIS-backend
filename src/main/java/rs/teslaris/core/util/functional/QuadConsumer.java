package rs.teslaris.core.util.functional;

@FunctionalInterface
public interface QuadConsumer<A, B, C, D> {
    void accept(A a, B b, C c, D d);

    default QuadConsumer<A, B, C, D> andThen(QuadConsumer<A, B, C, D> after) {
        return (a, b, c, d) -> {
            accept(a, b, c, d);
            after.accept(a, b, c, d);
        };
    }
}
