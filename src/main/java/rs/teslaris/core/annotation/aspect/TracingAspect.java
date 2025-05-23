package rs.teslaris.core.annotation.aspect;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;
import rs.teslaris.core.util.tracing.TraceMDCKeys;

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

        Class<?> targetClass = joinPoint.getTarget().getClass();
        String method = joinPoint.getSignature().toShortString();
        String args = Arrays.toString(joinPoint.getArgs());

        boolean isRestController = targetClass.isAnnotationPresent(RestController.class);

        populateMDC(isRestController);
        ensureTracingContextExists();

        // Pull context values
        String clientIp = MDC.get(TraceMDCKeys.CLIENT_IP);
        String tracingContextId = MDC.get(TraceMDCKeys.TRACING_CONTEXT_ID);
        String trackingCookieValue = MDC.get(TraceMDCKeys.SESSION);
        String returnType =
            ((MethodSignature) joinPoint.getSignature()).getReturnType().getSimpleName();

        log.debug(
            "TRACING - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - CLASS: {} - CALLED: {} - ARGS: {}",
            tracingContextId, trackingCookieValue,
            clientIp, targetClass.getSimpleName(),
            method, args);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            log.debug(
                "TRACING - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - CLASS: {} - RETURNED: {} - RETURN TYPE: {} - TOOK: {} ms - RESULT: {}",
                tracingContextId, trackingCookieValue,
                clientIp, targetClass.getSimpleName(),
                method, returnType, duration, result);
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;

            log.debug(
                "TRACING - CONTEXT: {} - TRACKING_COOKIE: {} - IP: {} - CLASS: {} - EXCEPTION IN: {} - ARGS: {} - TOOK: {} ms",
                tracingContextId, trackingCookieValue,
                clientIp, targetClass.getSimpleName(),
                method, args, duration, t);
            throw t;
        } finally {
            // Cleanup to prevent context memory leak
            MDC.remove(TraceMDCKeys.CLIENT_IP);
            MDC.remove(TraceMDCKeys.SESSION);
            MDC.remove(TraceMDCKeys.TRACING_CONTEXT_ID);
        }
    }

    private void populateMDC(boolean isRestController) {
        if (isRestController) {
            String clientIp = extractClientIp();
            MDC.put(TraceMDCKeys.CLIENT_IP, Objects.nonNull(clientIp) ? clientIp : "N/A");

            String trackingCookieValue = SessionTrackingUtil.getJSessionId();
            MDC.put(TraceMDCKeys.SESSION, trackingCookieValue);
        } else {
            if (Objects.isNull(MDC.get(TraceMDCKeys.SESSION))) {
                MDC.put(TraceMDCKeys.SESSION, "Server-side");
                MDC.put(TraceMDCKeys.CLIENT_IP, "N/A");
            }
        }
    }

    private void ensureTracingContextExists() {
        if (Objects.isNull(MDC.get(TraceMDCKeys.TRACING_CONTEXT_ID))) {
            MDC.put(TraceMDCKeys.TRACING_CONTEXT_ID, UUID.randomUUID().toString());
        }
    }

    @Nullable
    private String extractClientIp() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            var request = servletRequestAttributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");

            if (Objects.isNull(ip) || ip.isBlank()) {
                ip = request.getRemoteAddr();
            } else {
                // If multiple IPs (e.g., X-Forwarded-For: client, proxy1, proxy2)
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
        return null;
    }
}
