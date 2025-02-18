package rs.teslaris.core.assessment.controller;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.model.ReportType;
import rs.teslaris.core.assessment.service.interfaces.ReportingService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/report")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    private final JwtUtil tokenUtil;

    @PostMapping("/schedule-generation")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_REPORT_GENERATION')")
    public void scheduleReportGeneration(
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("type") ReportType reportType,
        @RequestParam("commissionId") Integer commissionId,
        @RequestParam("year") Integer year,
        @RequestParam("lang") String lang,
        @RequestHeader("Authorization") String bearerToken) {
        reportingService.scheduleReportGeneration(timestamp, reportType, year, commissionId, lang,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/{commissionId}")
    @PreAuthorize("hasAuthority('SCHEDULE_REPORT_GENERATION')")
    public List<String> getAvailableReportsForCommission(@PathVariable Integer commissionId) {
        return reportingService.getAvailableReportsForCommission(commissionId);
    }
}
