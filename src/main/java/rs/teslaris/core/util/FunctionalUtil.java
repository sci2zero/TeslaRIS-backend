package rs.teslaris.core.util;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class FunctionalUtil {

    public static <T> void forEachWithCounter(Iterable<T> source, BiConsumer<Integer, T> consumer) {
        int i = 0;
        for (T item : source) {
            consumer.accept(i, item);
            i++;
        }
    }

    public static <T> void forEachChunked(Pageable initialPageable,
                                          Function<Pageable, Page<T>> pageFetcher,
                                          Consumer<List<T>> pageProcessor) {
        var pageable = initialPageable;
        while (true) {
            Page<T> page = pageFetcher.apply(pageable);
            List<T> content = page.getContent();
            if (content.isEmpty()) {
                break;
            }

            pageProcessor.accept(content);
            if (!page.hasNext()) {
                break;
            }

            pageable = pageable.next();
        }
    }
}
