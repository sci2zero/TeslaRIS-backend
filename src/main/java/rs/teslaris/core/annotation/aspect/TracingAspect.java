package rs.teslaris.core.annotation.aspect;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.SessionUtil;

@Aspect
@Component
@Slf4j
public class TracingAspect {

    @Value("${tracing.enabled}")
    private Boolean tracingEnabled;

    @Around("@within(rs.teslaris.core.annotation.Traceable)")
    public Object traceMethodCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!tracingEnabled) {
            return joinPoint.proceed();
        }

        String method = joinPoint.getSignature().toShortString();
        String args = Arrays.toString(joinPoint.getArgs());
        String className = joinPoint.getTarget().getClass().getSimpleName();

        var returnType =
            ((MethodSignature) joinPoint.getSignature()).getReturnType().getSimpleName();
        var trackingCookieValue = SessionUtil.getJSessionId();

        MDC.put("session", trackingCookieValue); // enrich log context

        log.debug("TRACING - {} - CLASS: {} - CALLED: {} - ARGS: {}", trackingCookieValue,
            className,
            method, args);

        long start = System.currentTimeMillis();
        try {
            var result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            log.debug(
                "TRACING - {} - CLASS: {} - RETURNED: {} - RETURN TYPE: {} - TOOK: {} ms - RESULT: {}",
                trackingCookieValue, className, method, returnType, duration, result);
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            log.debug("TRACING - {} - CLASS: {} - EXCEPTION IN: {} - ARGS: {} - TOOK: {} ms",
                trackingCookieValue, className, method, args, duration, t);
            throw t;
        } finally {
            MDC.remove("session"); // Clean up to avoid context leak
        }
    }
}
