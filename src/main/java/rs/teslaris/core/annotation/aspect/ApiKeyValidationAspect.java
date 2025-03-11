package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.annotation.ApiKeyValidation;
import rs.teslaris.core.service.interfaces.commontypes.ApiKeyService;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidApiKeyException;

@Aspect
@Component
public class ApiKeyValidationAspect {

    private final ApiKeyService apiKeyService;

    private final Map<String, Integer> tokenBuckets = new ConcurrentHashMap<>();

    private final int MAX_TOKENS = 20; // Max requests per API key at any moment

    private final int REFILL_RATE = 1; // 1 token per second


    @Autowired
    public ApiKeyValidationAspect(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
        startTokenRefillTask();
    }

    private void startTokenRefillTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            tokenBuckets.forEach((apiKey, tokens) -> {
                if (tokens < MAX_TOKENS) {
                    tokenBuckets.put(apiKey, tokens + REFILL_RATE);
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Around("@annotation(rs.teslaris.core.annotation.ApiKeyValidation)")
    public Object checkPublicationEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        String apiKeyValue = request.getHeader("X-Api-Key");
        var method = AspectUtil.getMethod(joinPoint);
        var annotation = method.getAnnotation(ApiKeyValidation.class);

        if (Objects.isNull(apiKeyValue) ||
            !apiKeyService.validateApiKey(apiKeyValue, annotation.value())) {
            throw new InvalidApiKeyException(
                "API key is invalid, expired, or you have exceeded daily requests.");
        }

        // Token bucket rate limiting
        tokenBuckets.putIfAbsent(apiKeyValue, MAX_TOKENS);
        synchronized (tokenBuckets) {
            int remainingTokens = tokenBuckets.get(apiKeyValue);
            if (remainingTokens <= 0) {
                throw new InvalidApiKeyException("Rate limit exceeded. Try again later.");
            }
            tokenBuckets.put(apiKeyValue, remainingTokens - 1);
        }

        return joinPoint.proceed();
    }
}
