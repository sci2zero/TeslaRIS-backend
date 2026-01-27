package rs.teslaris.core.controller.utility;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.HealthCheckService;

@RestController
@RequestMapping("/api/health-check")
@RequiredArgsConstructor
@Traceable
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.git.tag:}")
    private String appGitTag;

    @Value("${app.git.commit.hash:}")
    private String appGitCommitHash;

    @Value("${app.git.repo.url:}")
    private String appGitRepoUrl;


    @GetMapping
    @PreAuthorize("hasAuthority('PERFORM_HEALTH_CHECK')")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("appVersion", appVersion);

        status.put("Postgres", healthCheckService.checkPostgres());
        status.put("Elasticsearch", healthCheckService.checkElasticsearch());
        status.put("MinIO", healthCheckService.checkMinio());
        status.put("Mail Server", healthCheckService.checkMail());
        status.put("MongoDB", healthCheckService.checkMongo());

        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("appVersion", appVersion);
        status.put("appGitRepoUrl", appGitRepoUrl);
        status.put("appGitTag", appGitTag);
        status.put("appGitCommitHash", appGitCommitHash);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }
}
