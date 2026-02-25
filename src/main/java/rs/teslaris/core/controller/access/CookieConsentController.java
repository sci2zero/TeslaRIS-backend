package rs.teslaris.core.controller.access;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.configuration.TrackingCookieConfiguration;

@RestController
@RequestMapping("/api/cookie")
@RequiredArgsConstructor
@Traceable
public class CookieConsentController {

    public final TrackingCookieConfiguration trackingCookieConfiguration;

    @PatchMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> optOut(HttpServletRequest request, HttpServletResponse response,
                                       @RequestParam Boolean optOut) {
        var cookie = new Cookie("tracking_opt_out", optOut.toString());
        cookie.setPath("/");
        cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        if (optOut) {
            response.addCookie(
                trackingCookieConfiguration.getCookieForUnsetting()); // unset current tracking cookies
        } else if (Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
            .noneMatch(c -> c.getName().equals("tracking_opt_out"))) {
            response.addCookie(trackingCookieConfiguration.getTrackingCookie());
        }

        return ResponseEntity.noContent().build();
    }
}
