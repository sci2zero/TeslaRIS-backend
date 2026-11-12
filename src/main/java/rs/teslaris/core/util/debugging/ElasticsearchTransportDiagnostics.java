package rs.teslaris.core.util.debugging;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes the state of the JVM to the log when the application finds a dead Elasticsearch
 * transport.
 */
@Slf4j
public class ElasticsearchTransportDiagnostics {

    private static final long MEGABYTE = 1024L * 1024L;

    private static final List<String> WATCHED_THREAD_PREFIXES =
        List.of("I/O dispatcher", "elasticsearch-rest-client", "Async-", "ReindexThread-",
            "TaskScheduler-", "http-nio-");


    public static void logSnapshot(String reason) {
        var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        var threads = ManagementFactory.getThreadMXBean();

        log.error("""
                JVM snapshot ({}):
                  uptime: {}
                  heap: {} MB used / {} MB committed / {} MB max
                  threads: {} live, {} peak, by pool {}
                  gc: {}""",
            reason,
            Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()),
            heap.getUsed() / MEGABYTE, heap.getCommitted() / MEGABYTE, heap.getMax() / MEGABYTE,
            threads.getThreadCount(), threads.getPeakThreadCount(), threadCountsByPool(),
            garbageCollectorSummary());
    }

    private static Map<String, Long> threadCountsByPool() {
        return Arrays.stream(ManagementFactory.getThreadMXBean().dumpAllThreads(false, false))
            .map(ThreadInfo::getThreadName)
            .map(ElasticsearchTransportDiagnostics::poolOf)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(pool -> pool, TreeMap::new, Collectors.counting()));
    }

    private static String poolOf(String threadName) {
        return WATCHED_THREAD_PREFIXES.stream()
            .filter(threadName::contains)
            .findFirst()
            .orElse(null);
    }

    private static String garbageCollectorSummary() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(gc -> gc.getName() + "=" + gc.getCollectionCount() + " collections/"
                + gc.getCollectionTime() + " ms")
            .collect(Collectors.joining(", "));
    }
}
