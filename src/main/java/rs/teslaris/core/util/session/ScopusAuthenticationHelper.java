package rs.teslaris.core.util.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ScopusAuthenticationHelper {

    public final RestTemplate restTemplate;

    public Map<String, String> headers = new HashMap<>();

    private boolean authenticated = false;

    @Value("${scopus.api.key}")
    private String apiKey;


    @Autowired
    public ScopusAuthenticationHelper(@Value("${scopus.proxy.enabled:false}") boolean proxyEnabled,
                                      @Value("${scopus.proxy.host:}") String proxyHost,
                                      @Value("${scopus.proxy.port:0}") int proxyPort) {
        restTemplate = constructRestTemplate(proxyEnabled, proxyHost, proxyPort);
    }

    public boolean authenticate() {
        if (authenticated) {
            return true;
        }

        headers.put("X-ELS-APIKey", apiKey);
        var url = "https://api.elsevier.com/authenticate?platform=SCOPUS";

        var requestHeaders = new HttpHeaders();
        headers.forEach(requestHeaders::add);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders),
                    String.class);
        } catch (HttpClientErrorException | ResourceAccessException e) {
            log.error("Exception occurred when authenticating to Scopus: {}", e.getMessage());
            return false;
        }

        if (responseEntity.getStatusCode() == HttpStatus.MULTIPLE_CHOICES) {
            var objectMapper = new ObjectMapper();
            try {
                var response =
                    objectMapper.readValue(responseEntity.getBody(), AuthenticateResponse.class);
                var idString = response.pathChoices().choice().get(1).id();
                var choice = Integer.parseInt(idString);
                var isSuccess = getAuthtoken(choice, headers);
                if (isSuccess) {
                    authenticated = true;
                }
            } catch (HttpClientErrorException | JsonProcessingException e) {
                log.error("Exception occurred: {}", e.getMessage());
            }
        } else if (responseEntity.getStatusCode() == HttpStatus.OK) {
            authenticated = setAuthToken(responseEntity);
        } else {
            log.error("Exception occurred: {}", responseEntity.getStatusCode());
        }

        return authenticated;
    }

    public boolean refreshAuthentication() {
        this.authenticated = false;

        return authenticate();
    }

    private boolean getAuthtoken(int choice, Map<String, String> headers) {
        var url = "https://api.elsevier.com/authenticate?platform=SCOPUS&choice=" + choice;

        var requestHeaders = new HttpHeaders();
        headers.forEach(requestHeaders::add);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders),
                    String.class);
        } catch (HttpClientErrorException | ResourceAccessException e) {
            log.error("Exception occurred during auth token fetching: {}", e.getMessage());
            return false;
        }

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return setAuthToken(responseEntity);
        } else {
            log.error("Exception occurred during auth token fetching, status code: {}",
                responseEntity.getStatusCode());
        }

        return false;
    }

    private boolean setAuthToken(ResponseEntity<String> responseEntity) {
        var objectMapper = new ObjectMapper();
        try {
            var response =
                objectMapper.readValue(responseEntity.getBody(), AuthTokenResponse.class);
            var authtoken = response.authToken().authtoken();
            headers.put("X-ELS-Authtoken", authtoken);
            return true;
        } catch (HttpClientErrorException | JsonProcessingException e) {
            log.error("Exception occurred during auth token deserialization: {}",
                e.getMessage());
        }

        return false;
    }

    private RestTemplate constructRestTemplate(boolean proxyEnabled, String proxyHost,
                                               int proxyPort) {
        var config = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(10))
            .setConnectTimeout(Timeout.ofSeconds(10))
            .setResponseTimeout(Timeout.ofSeconds(30))
            .build();

        var httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(config);

        if (proxyEnabled && Objects.nonNull(proxyHost) && !proxyHost.isBlank() && proxyPort > 0) {
            httpClientBuilder.setProxy(new HttpHost("http", proxyHost, proxyPort));
        }

        var requestFactory =
            new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());

        return new RestTemplate(requestFactory);
    }

    private record AuthenticateResponse(
        @JsonProperty("pathChoices") PathChoices pathChoices
    ) {
    }

    private record PathChoices(
        @JsonProperty("choice") List<Choice> choice
    ) {
    }

    private record Choice(
        @JsonProperty("@id") String id
    ) {
    }

    private record AuthTokenResponse(
        @JsonProperty("authenticate-response") AuthToken authToken
    ) {
    }

    private record AuthToken(
        @JsonProperty("@choice") String choice,
        @JsonProperty("@type") String type,
        @JsonProperty("authtoken") String authtoken
    ) {
    }
}
