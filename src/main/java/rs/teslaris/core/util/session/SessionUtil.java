package rs.teslaris.core.util.session;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.util.jwt.JwtUtil;

@Component
@Slf4j
public class SessionUtil {

    private static JwtUtil tokenUtil;


    @Autowired
    public SessionUtil(JwtUtil tokenUtil) {
        SessionUtil.tokenUtil = tokenUtil;
    }

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

    public static String getCurrentClientIP() {
        try {
            return Objects.requireNonNullElse(MDC.get(TraceMDCKeys.CLIENT_IP), "N/A");
        } catch (IllegalArgumentException e) {
            return "N/A";
        }
    }

    public static String getCurrentClientUserAgent() {
        try {
            return Objects.requireNonNullElse(MDC.get(TraceMDCKeys.USER_AGENT), "N/A");
        } catch (IllegalArgumentException e) {
            return "N/A";
        }
    }

    public static String getCurrentTracingContextId() {
        try {
            var tracingContextID = MDC.get(TraceMDCKeys.TRACING_CONTEXT_ID);

            if (Objects.isNull(tracingContextID)) {
                return "N/A";
            }

            return tracingContextID;
        } catch (IllegalArgumentException e) {
            log.error("CRITICAL - Unable to obtain MDC tracing context id.");
            return "N/A";
        }
    }

    public static boolean isUserLoggedIn() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return !(Objects.isNull(authentication) || !authentication.isAuthenticated() ||
            (authentication.getPrincipal() instanceof String &&
                authentication.getPrincipal().equals("anonymousUser")));
    }

    public static boolean isUserLoggedInAndAdmin() {
        return isUserLoggedIn() &&
            getLoggedInUser().getAuthority().getName().equals(UserRole.ADMIN.name());
    }

    @Nullable
    public static User getLoggedInUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isUserLoggedIn()) {
            return null;
        }

        return (User) authentication.getPrincipal();
    }

    public static boolean isSessionValid(HttpServletRequest request, String bearerToken) {
        if (Objects.isNull(bearerToken) || !isUserLoggedIn()) {
            return false;
        }

        String fingerprintCookie = extractCookieValue(request, "jwt-security-fingerprint");
        if (Objects.isNull(fingerprintCookie) || fingerprintCookie.isBlank()) {
            return false;
        }

        var token = bearerToken.split(" ")[1];
        var userDetails = (UserDetails) getLoggedInUser();

        return tokenUtil.validateToken(token, userDetails, fingerprintCookie);
    }

    public static boolean hasAnyRole(String bearerToken, List<UserRole> roles) {
        var role = tokenUtil.extractUserRoleFromToken(bearerToken);

        try {
            if (roles.contains(UserRole.valueOf(role))) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        return false;
    }

    private static String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (Objects.nonNull(cookies)) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
