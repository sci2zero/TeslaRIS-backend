package rs.teslaris.core.util;

import java.util.Objects;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SessionUtil {

    public static String getJSessionId() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(attrs)) {
            return "Anonymous";
        }

        var request = attrs.getRequest();
        if (Objects.isNull(request.getCookies())) {
            return "Anonymous";
        }

        for (var cookie : request.getCookies()) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return "Anonymous";
    }
}
