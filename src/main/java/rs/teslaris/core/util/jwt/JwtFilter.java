package rs.teslaris.core.util.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil tokenUtil;
    private final UserDetailsService userDetailsService;
    @Value("${jwt.header.string}")
    public String headerString;
    @Value("${jwt.token.prefix}")
    public String tokenPrefix;

    protected JwtFilter(JwtUtil tokenUtil, UserDetailsService userDetailsService) {
        this.tokenUtil = tokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Cookie[] cookies = request.getCookies();
        String cookieValue = null;
        if (cookies != null) {
            cookieValue = this.getCookieValue(cookies, "jwt-security-fingerprint");
        }

        String jwt = this.getJwtFromRequestHeader(request);
        if (jwt != null && cookieValue != null) {
            this.validateToken(jwt, cookieValue);
        }

        filterChain.doFilter(request, response);
    }

    protected String getCookieValue(Cookie[] cookies, String cookieName) {
        var optionalCookie =
            Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(cookieName))
                .findFirst();
        if (optionalCookie.isEmpty()) {
            return null;
        }
        return optionalCookie.get().getValue();
    }

    private String getJwtFromRequestHeader(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(headerString);
        if (authorizationHeader == null) {
            return null;
        } else if (authorizationHeader.startsWith(tokenPrefix)) {
            return authorizationHeader.replace(tokenPrefix, "");
        }
        return null;
    }

    private void validateToken(String jwt, String cookieValue) {
        try {
            String username = tokenUtil.extractUsernameFromToken(jwt);

            var res = tokenUtil.checkAlgHeaderParam(jwt);
            if (!res) {
                throw new MalformedJwtException(String.format("JWT signature algorithm is not %s.",
                    JwtUtil.signatureAlgorithm.getValue()));
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (Boolean.TRUE.equals(tokenUtil.validateToken(jwt, userDetails, cookieValue))) {
                var authenticationToken = new TokenBasedAuthentication(userDetails);
                authenticationToken.setToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (IllegalArgumentException |
                 SignatureException |
                 ExpiredJwtException |
                 MalformedJwtException |
                 UnsupportedJwtException |
                 UsernameNotFoundException |
                 InvalidCookieException ignored) {
            log.error(ignored.getMessage());
        }
    }


}
