package rs.teslaris.core.controller.access;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rs.teslaris.core.service.interfaces.commontypes.ProgressService;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SSEController {

    private final ProgressService progressService;

    private final Cache<String, String> tokenStore =
        CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    @GetMapping(value = "/progress/{exportId}",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String exportId,
                                     @RequestParam String accessToken) {
        if (!tokenStore.asMap().containsKey(accessToken) ||
            !tokenStore.asMap().get(accessToken).equals(exportId)) {
            throw new AccessDeniedException(
                "You need to provide valid accessToken + exportId combination.");
        }

        tokenStore.asMap().remove(accessToken);
        return progressService.register(exportId);
    }

    @GetMapping(value = "/access-token")
    public String generateSseAccessToken(@RequestParam String exportId) {
        var token = UUID.randomUUID().toString();
        tokenStore.put(token, exportId);

        return token;
    }
}
