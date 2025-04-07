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
import org.springframework.beans.factory.annotation.Value;
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

    private final int maxTokens;

    private final int refillRate;

    private final int refillPeriod;


    @Autowired
    public ApiKeyValidationAspect(@Value("${rate-limiting.max-tokens}") int maxTokens,
                                  @Value("${rate-limiting.refill-rate}") int refillRate,
                                  @Value("${rate-limiting.refill-period-seconds}") int refillPeriod,
                                  ApiKeyService apiKeyService) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
        this.refillPeriod = refillPeriod;
        this.apiKeyService = apiKeyService;
        startTokenRefillTask();
    }

    private void startTokenRefillTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            tokenBuckets.forEach((apiKey, tokens) -> {
                if (tokens < maxTokens) {
                    tokenBuckets.put(apiKey, tokens + refillRate);
                }
            });
        }, 0, refillPeriod, TimeUnit.SECONDS);
    }

    @Around("@annotation(rs.teslaris.core.annotation.ApiKeyValidation)")
    public Object validateAPIKey(ProceedingJoinPoint joinPoint) throws Throwable {
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
        tokenBuckets.putIfAbsent(apiKeyValue, maxTokens);
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
