package rs.teslaris.core.annotation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

public class AspectUtil {

    public static Method getMethod(ProceedingJoinPoint joinPoint) {
        var signature = joinPoint.getSignature();
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalArgumentException("Aspect should be applied to a method.");
        }
        return ((MethodSignature) signature).getMethod();
    }

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes()))
            .getRequest();
    }

    public static String extractToken(HttpServletRequest request) {
        var bearerToken = request.getHeader("Authorization");
        return bearerToken.split(" ")[1];
    }

    public static Map<String, String> getUriVariables(HttpServletRequest request) {
        return (Map<String, String>) request.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }
}
