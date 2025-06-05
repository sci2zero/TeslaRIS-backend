package rs.teslaris.core.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TrackingCookieConfiguration extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "JSESSIONID";

    private static final String OPT_OUT_COOKIE = "tracking_opt_out";

    private static final int TRACKING_COOKIE_MAX_AGE = 3600;

    private final Environment environment;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
        throws ServletException, IOException {

        var hasSessionId =
            Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .anyMatch(cookie -> COOKIE_NAME.equals(cookie.getName()));

        var optedOut =
            Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> OPT_OUT_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .map("true"::equals)
                .findFirst()
                .orElse(true);

        if (!hasSessionId && !optedOut) {
            var sessionId = UUID.randomUUID().toString();
            var cookie = createCookie(sessionId);

            cookie.setMaxAge(TRACKING_COOKIE_MAX_AGE);
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }

    private Cookie createCookie(String sessionId) {
        var cookie = new Cookie(COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);

        if (Arrays.stream(environment.getActiveProfiles())
            .noneMatch(profile -> profile.equalsIgnoreCase("test"))) {
            cookie.setSecure(true);
        }

        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");

        return cookie;
    }

    public Cookie getCookieForUnsetting() {
        var cookie = createCookie(null);
        cookie.setMaxAge(0);
        return cookie;
    }

    public Cookie getTrackingCookie() {
        var cookie = createCookie(UUID.randomUUID().toString());
        cookie.setMaxAge(TRACKING_COOKIE_MAX_AGE);
        return cookie;
    }
}

