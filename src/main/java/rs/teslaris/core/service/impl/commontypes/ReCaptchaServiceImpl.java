package rs.teslaris.core.service.impl.commontypes;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;

@Service
@Traceable
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

    @Autowired
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
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("secret", secretKey);
        params.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        var restTemplate = new RestTemplate();
        var response = restTemplate.postForEntity(VERIFY_URL, request, Map.class);

        return response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"));
    }

}
