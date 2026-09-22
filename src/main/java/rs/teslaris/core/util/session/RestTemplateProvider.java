package rs.teslaris.core.util.session;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Objects;
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

    private final RestTemplate restTemplate;

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:0}")
    private int proxyPort;

    @Value("${proxy.type:HTTP}") // HTTP or SOCKS
    private String proxyType;


    @Autowired
    public RestTemplateProvider(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .requestFactory(this::createRequestFactory)
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

    private SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10 * 1000);
        factory.setReadTimeout(20 * 1000);

        if (proxyEnabled && Objects.nonNull(proxyHost) && proxyPort > 0) {
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
}
