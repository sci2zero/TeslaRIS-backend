package rs.teslaris.core.util.debugging;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceLoggingUtil {

    public static <T> T timed(Supplier<T> operation, String name) {
        long start = System.nanoTime();
        T result = operation.get();
        log.info("{} took {} ms", name, (System.nanoTime() - start) / 1_000_000.0);
        return result;
    }

    public static void timed(Runnable operation, String name) {
        long start = System.nanoTime();
        operation.run();
        log.info("{} took {} ms", name, (System.nanoTime() - start) / 1_000_000.0);
    }
}
