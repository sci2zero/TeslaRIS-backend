package rs.teslaris.core.annotation.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class AspectUtil {

    public static Method getMethod(ProceedingJoinPoint joinPoint) {
        var signature = joinPoint.getSignature();
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalArgumentException("Aspect should be applied to a method.");
        }
        return ((MethodSignature) signature).getMethod();
    }
}
