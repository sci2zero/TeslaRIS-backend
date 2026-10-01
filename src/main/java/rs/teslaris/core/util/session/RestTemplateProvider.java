package rs.teslaris.core.util.session;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateProvider {

    private static final long DEFAULT_BACKOFF_MILLIS = 2000;

    private static final long MAX_BACKOFF_MILLIS = 10000;

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10 * 1000;

    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000;

    private final RestTemplateBuilder restTemplateBuilder;

    private final RestTemplate restTemplate;

    private final Map<Integer, RestTemplate> restTemplatesByReadTimeout =
        new ConcurrentHashMap<>();

    private final boolean proxyEnabled;

    private final String proxyHost;

    private final int proxyPort;

    private final String proxyType;


    /**
     * Proxy settings are constructor injected on purpose: {@code RestTemplateBuilder} invokes the
     * request factory supplier during {@code build()}, which runs before field injection would
     * have populated them.
     */
    @Autowired
    public RestTemplateProvider(RestTemplateBuilder restTemplateBuilder,
                                @Value("${proxy.enabled:false}") boolean proxyEnabled,
                                @Value("${proxy.host:}") String proxyHost,
                                @Value("${proxy.port:0}") int proxyPort,
                                @Value("${proxy.type:HTTP}") String proxyType) { // HTTP or SOCKS
        this.restTemplateBuilder = restTemplateBuilder;
        this.proxyEnabled = proxyEnabled;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyType = proxyType;
        this.restTemplate = restTemplateBuilder
            .requestFactory(() -> createRequestFactory(DEFAULT_READ_TIMEOUT_MILLIS))
            .build();
    }

    public static void sleepBeforeRetry(String retryAfterHeader) {
        var backoffMillis = DEFAULT_BACKOFF_MILLIS;

        if (Objects.nonNull(retryAfterHeader)) {
            try {
                backoffMillis =
                    Math.min(Long.parseLong(retryAfterHeader.trim()) * 1000, MAX_BACKOFF_MILLIS);
            } catch (NumberFormatException ignored) {
                backoffMillis = DEFAULT_BACKOFF_MILLIS;
            }
        }

        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory(int readTimeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(readTimeoutMillis);

        if (proxyEnabled && Objects.nonNull(proxyHost) && !proxyHost.isBlank() && proxyPort > 0) {
            Proxy.Type type =
                "SOCKS".equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }

        return factory;
    }

    public RestTemplate provideRestTemplate() {
        return restTemplate;
    }

    /**
     * Provides a template with an overridden read timeout, for callers that talk to services
     * slower than the shared default allows. Non-positive values fall back to the default template.
     */
    public RestTemplate provideRestTemplate(int readTimeoutMillis) {
        if (readTimeoutMillis <= 0 || readTimeoutMillis == DEFAULT_READ_TIMEOUT_MILLIS) {
            return restTemplate;
        }

        return restTemplatesByReadTimeout.computeIfAbsent(readTimeoutMillis,
            timeout -> restTemplateBuilder
                .requestFactory(() -> createRequestFactory(timeout))
                .build());
    }
}
