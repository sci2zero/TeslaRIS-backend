package rs.teslaris.core.service.impl.commontypes;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;

@Service
public class ReCaptchaServiceImpl implements ReCaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret.key}")
    private String secretKey;

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.host:}")
    private String proxyHost;

    @Value("${proxy.port:0}")
    private int proxyPort;

    @Value("${proxy.type:HTTP}") // HTTP or SOCKS
    private String proxyType;

    public ReCaptchaServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .requestFactory(this::createRequestFactory)
            .build();
    }

    private SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        if (proxyEnabled && proxyHost != null && proxyPort > 0) {
            Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType) ? Type.SOCKS : Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }

        return factory;
    }

    @Override
    public boolean isCaptchaValid(String token) {
        var params = Map.of("secret", secretKey, "response", token);
        var response = restTemplate.postForObject(VERIFY_URL, params, Map.class);
        return response != null && Boolean.TRUE.equals(response.get("success"));
    }
}
