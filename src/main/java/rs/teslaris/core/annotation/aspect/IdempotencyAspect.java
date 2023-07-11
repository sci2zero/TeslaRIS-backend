package rs.teslaris.core.annotation.aspect;

import com.google.common.cache.Cache;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.util.exceptionhandling.exception.IdempotencyException;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final Cache<String, Byte> idempotencyCacheStore;


    @Around("@annotation(rs.teslaris.core.annotation.Idempotent)")
    public Object checkIdempotencyKey(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();

        var idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IdempotencyException("You have to provide an idempotency key.");
        }

        if (idempotencyCacheStore.getIfPresent(idempotencyKey) == null) {
            idempotencyCacheStore.put(idempotencyKey, (byte) 1);
        } else {
            throw new IdempotencyException("Idempotency key allready in use.");
        }

        return joinPoint.proceed();
    }
}
