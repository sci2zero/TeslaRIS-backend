package rs.teslaris.core.controller;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rs.teslaris.core.service.interfaces.commontypes.ProgressService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SSEController {

    private static final long TOKEN_EXPIRY_MINUTES = 5;

    private final ProgressService progressService;

    private final JwtUtil tokenUtil;

    private final Map<String, Integer> tokenStore = new ConcurrentHashMap<>();


    @GetMapping(value = "/progress/{exportId}",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String exportId) {
        return progressService.register(exportId);
    }

    private String generateSseToken(String jwt, String exportId) {
        var userId = tokenUtil.extractUserIdFromToken(jwt);

        var token = UUID.randomUUID().toString();
        var expiry = Instant.now().plusSeconds(TOKEN_EXPIRY_MINUTES * 60);

        tokenStore.put(token, userId);

        return token;
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
//        tokenStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiry()));
    }
}
