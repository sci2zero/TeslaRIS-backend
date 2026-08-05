package rs.teslaris.migrator.client.hydrator;

import feign.RequestInterceptor;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.util.Date;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Per-client Feign configuration. Intentionally not annotated with {@code @Configuration} so it does
 * not leak into every other Feign client.
 */
public class HydratorFeignConfiguration {

    @Value("${migrator.sources.hydrator.api-key-header:X-API-KEY}")
    private String apiKeyHeader;

    @Value("${migrator.sources.hydrator.api-key:}")
    private String apiKey;


    @Bean
    public RequestInterceptor hydratorApiKeyInterceptor() {
        return template -> {
            if (Objects.nonNull(apiKey) && !apiKey.isBlank()) {
                template.header(apiKeyHeader, apiKey);
            }
        };
    }

    /**
     * 5xx and 429 are worth another attempt (the hydrator rate limits per API key); everything else
     * is terminal and is reported to the failure handler as-is.
     */
    @Bean
    public ErrorDecoder hydratorErrorDecoder() {
        var defaultDecoder = new ErrorDecoder.Default();

        return (methodKey, response) -> {
            var exception = defaultDecoder.decode(methodKey, response);

            if (isRetryable(response)) {
                return new RetryableException(response.status(), exception.getMessage(),
                    response.request().httpMethod(), exception, (Date) null, response.request());
            }

            return exception;
        };
    }

    private boolean isRetryable(Response response) {
        return response.status() >= 500 || response.status() == 429;
    }
}
