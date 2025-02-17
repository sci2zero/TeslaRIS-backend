package rs.teslaris.core.util;

import java.util.Objects;

public class Triple<A, B, C> {
    public final A a;
    public final B b;
    public final C c;

    public Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static <P, Q, R> Triple<P, Q, R> makeTriple(P p, Q q, R r) {
        return new Triple<>(p, q, r);
    }

    @SuppressWarnings("unchecked")
    public static <P, Q, R> Triple<P, Q, R> cast(Triple<?, ?, ?> triple, Class<P> pClass,
                                                 Class<Q> qClass, Class<R> rClass) {
        if (triple.isInstance(pClass, qClass, rClass)) {
            return (Triple<P, Q, R>) triple;
        }
        throw new ClassCastException();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((b == null) ? 0 : b.hashCode());
        result = prime * result + ((c == null) ? 0 : c.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        Triple other = (Triple) obj;
        return (Objects.equals(a, other.a)) &&
            (Objects.equals(b, other.b)) &&
            (Objects.equals(c, other.c));
    }

    public boolean isInstance(Class<?> classA, Class<?> classB, Class<?> classC) {
        return classA.isInstance(a) && classB.isInstance(b) && classC.isInstance(c);
    }
}

