package rs.teslaris.core.util.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Slf4j
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

    public static <T> Stream<List<T>> batch(Stream<T> stream, int size) {
        var iterator = stream.iterator();

        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
            false
        ).map(e -> {
            var batch = new ArrayList<T>(size);
            batch.add(e);

            for (int i = 1; i < size && iterator.hasNext(); i++) {
                batch.add(iterator.next());
            }
            return batch;
        });
    }

    public static <T extends BaseEntity> void processAllPages(
        int chunkSize,
        Sort sort,
        Function<PageRequest, Page<T>> pageSupplier,
        Consumer<T> itemProcessor
    ) {
        int pageNumber = 0;

        while (true) {
            Page<T> page = pageSupplier.apply(
                PageRequest.of(pageNumber, chunkSize, sort)
            );

            try {
                page.getContent().forEach(entity -> {
                    try {
                        itemProcessor.accept(entity);
                    } catch (Exception e) {
                        log.warn("Error processing {} ID {}: {}",
                            entity.getClass().getSimpleName(),
                            entity.getId(), e.getMessage(), e
                        );
                    }
                });
            } catch (Exception e) {
                log.warn("Skipping entire page {} due to error: {}",
                    pageNumber, e.getMessage());
            }

            if (!page.hasNext()) {
                break;
            }

            pageNumber++;
        }
    }
}
