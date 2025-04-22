package rs.teslaris.core.util;

import jakarta.annotation.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SessionUtil {

    @Nullable
    public static String getJSessionId() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }

        var request = attrs.getRequest();
        if (request.getCookies() == null) {
            return null;
        }

        for (var cookie : request.getCookies()) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
