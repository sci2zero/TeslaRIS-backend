package rs.teslaris.core.configuration;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.Endpoint;
import co.elastic.clients.transport.TransportOptions;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import rs.teslaris.core.util.debugging.ElasticsearchTransportDiagnostics;

/**
 * Replaces the Elasticsearch client after its I/O reactor thread stops, because the client cannot
 * recover without a new instance.
 */
@Slf4j
@SuppressWarnings("resource")
public class ResilientElasticsearchTransport implements ElasticsearchTransport {

    private static final int MAX_CAUSE_DEPTH = 10;

    private final Supplier<RestClient> restClientFactory;

    private final Function<RestClient, ElasticsearchTransport> transportFactory;

    private final Object rebuildLock = new Object();

    private final AtomicInteger rebuildCount = new AtomicInteger();

    private volatile Delegate delegate;

    private volatile Instant lastSuccessfulRequest = Instant.now();


    public ResilientElasticsearchTransport(RestClient initialRestClient,
                                           Supplier<RestClient> restClientFactory,
                                           Function<RestClient, ElasticsearchTransport> transportFactory) {
        this.restClientFactory = restClientFactory;
        this.transportFactory = transportFactory;
        this.delegate = new Delegate(transportFactory.apply(initialRestClient), initialRestClient);
    }

    @Override
    public <RequestT, ResponseT, ErrorT> ResponseT performRequest(RequestT request,
                                                                  Endpoint<RequestT, ResponseT, ErrorT> endpoint,
                                                                  TransportOptions options)
        throws IOException {
        var current = liveDelegate();

        try {
            var response = current.transport().performRequest(request, endpoint, options);
            lastSuccessfulRequest = Instant.now();
            return response;
        } catch (RuntimeException e) {
            if (!transportIsDead(current, e)) {
                throw e;
            }

            return rebuild(current, "a failed request", e).transport()
                .performRequest(request, endpoint, options);
        }
    }

    /**
     * The reactive client is the only user of this method, and this method does not retry a failed
     * future.
     */
    @Override
    public <RequestT, ResponseT, ErrorT> CompletableFuture<ResponseT> performRequestAsync(
        RequestT request, Endpoint<RequestT, ResponseT, ErrorT> endpoint,
        TransportOptions options) {
        return liveDelegate().transport().performRequestAsync(request, endpoint, options);
    }

    @Override
    public JsonpMapper jsonpMapper() {
        return delegate.transport().jsonpMapper();
    }

    @Override
    public TransportOptions options() {
        return delegate.transport().options();
    }

    @Override
    public void close() throws IOException {
        delegate.transport().close();
    }

    private Delegate liveDelegate() {
        var current = delegate;

        // The client throws a CancellationException when this flag is false.
        if (current.restClient().isRunning()) {
            return current;
        }

        return rebuild(current, "the liveness check", null);
    }

    private Delegate rebuild(Delegate dead, String trigger, RuntimeException failure) {
        synchronized (rebuildLock) {
            if (delegate != dead) {
                return delegate; // Another thread rebuilt the transport.
            }

            log.error(
                "Elasticsearch transport is dead, noticed by {}. Rebuild #{}. The last successful "
                    + "request was at {} ({} ago): the reactor thread died at or after that "
                    + "timestamp, so search the log from there for the root cause - an "
                    + "OutOfMemoryError, 'I/O reactor terminated abnormally', or a "
                    + "NoClassDefFoundError if the jar was replaced under the running JVM. This "
                    + "log line is only where the damage was noticed, never its cause.",
                trigger, rebuildCount.incrementAndGet(), lastSuccessfulRequest,
                Duration.between(lastSuccessfulRequest, Instant.now()), failure);

            ElasticsearchTransportDiagnostics.logSnapshot("Elasticsearch transport rebuild");

            var rebuilt = rebuiltDelegate();
            delegate = rebuilt;

            closeQuietly(dead.transport());

            log.warn("Elasticsearch transport rebuilt, requests should succeed again");
            return rebuilt;
        }
    }

    private Delegate rebuiltDelegate() {
        var rebuiltRestClient = restClientFactory.get();

        try {
            return new Delegate(transportFactory.apply(rebuiltRestClient), rebuiltRestClient);
        } catch (RuntimeException | Error e) {
            closeQuietly(rebuiltRestClient);
            throw e;
        }
    }

    private boolean transportIsDead(Delegate current, RuntimeException failure) {
        if (!current.restClient().isRunning()) {
            return true;
        }

        // RestClient puts the CancellationException into a RuntimeException, and the code must
        // examine the cause chain.
        var cause = (Throwable) failure;
        for (int depth = 0; Objects.nonNull(cause) && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof CancellationException) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private void closeQuietly(Closeable deadResource) {
        try {
            // A dead resource that stays open keeps its connection pool and its threads.
            deadResource.close();
        } catch (Exception e) {
            log.warn("Failed to close a dead Elasticsearch resource", e);
        }
    }

    private record Delegate(ElasticsearchTransport transport, RestClient restClient) {
    }
}
