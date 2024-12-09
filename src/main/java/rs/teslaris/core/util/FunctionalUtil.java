package rs.teslaris.core.util;

import java.util.function.BiConsumer;

public class FunctionalUtil {

    public static <T> void forEachWithCounter(Iterable<T> source, BiConsumer<Integer, T> consumer) {
        int i = 0;
        for (T item : source) {
            consumer.accept(i, item);
            i++;
        }
    }
}
