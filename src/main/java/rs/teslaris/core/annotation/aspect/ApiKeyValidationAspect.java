package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.annotation.ApiKeyValidation;
import rs.teslaris.core.service.interfaces.commontypes.ApiKeyService;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidApiKeyException;

@Aspect
@Component
@RequiredArgsConstructor
public class ApiKeyValidationAspect {

    private final ApiKeyService apiKeyService;

    @Around("@annotation(rs.teslaris.core.annotation.ApiKeyValidation)")
    public Object checkPublicationEdit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        String apiKeyValue = request.getHeader("X-Api-Key");
        var method = AspectUtil.getMethod(joinPoint);
        var annotation = method.getAnnotation(ApiKeyValidation.class);

        if (!apiKeyService.validateApiKey(apiKeyValue, annotation.value())) {
            throw new InvalidApiKeyException("Invalid API key");
        }

        return joinPoint.proceed();
    }
}
