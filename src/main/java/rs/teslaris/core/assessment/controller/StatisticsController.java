package rs.teslaris.core.assessment.controller;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.service.interfaces.statistics.StatisticsService;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final Bucket bucket;

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        var limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
            .addLimit(limit)
            .build();
    }

    @GetMapping("/{statisticsType}")
    public List<String> fetchStatisticsTypeIndicators(@PathVariable StatisticsType statisticsType) {
        return statisticsService.fetchStatisticsTypeIndicators(statisticsType);
    }

    @PostMapping("/person/{personId}")
    @Idempotent
    public ResponseEntity<Void> registerPersonView(@PathVariable Integer personId) {
        if (bucket.tryConsume(1)) {
            statisticsService.savePersonView(personId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("/organisation-unit/{organisationUnitId}")
    @Idempotent
    public ResponseEntity<Void> registerOrganisationUnitView(
        @PathVariable Integer organisationUnitId) {
        if (bucket.tryConsume(1)) {
            statisticsService.saveOrganisationUnitView(organisationUnitId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping("/document/{documentId}")
    @Idempotent
    public ResponseEntity<Void> registerDocumentView(@PathVariable Integer documentId) {
        if (bucket.tryConsume(1)) {
            statisticsService.saveDocumentView(documentId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
