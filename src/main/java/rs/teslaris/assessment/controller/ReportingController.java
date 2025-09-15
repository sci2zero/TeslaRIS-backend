package rs.teslaris.assessment.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import rs.teslaris.assessment.dto.ReportDTO;
import rs.teslaris.assessment.model.ReportType;
import rs.teslaris.assessment.service.interfaces.ReportingService;
import rs.teslaris.core.annotation.ReportGenerationCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.files.StreamingUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.session.SessionUtil;

@RestController
@RequestMapping("/api/assessment/report")
@RequiredArgsConstructor
@Traceable
public class ReportingController {

    private final ReportingService reportingService;

    private final JwtUtil tokenUtil;


    @PostMapping("/schedule-generation")
    @PreAuthorize("hasAuthority('SCHEDULE_REPORT_GENERATION')")
    @ReportGenerationCheck
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleReportGeneration(
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("type") ReportType reportType,
        @RequestParam("commissionId") List<Integer> commissionIds,
        @RequestParam("year") Integer year,
        @RequestParam("lang") String lang,
        @RequestParam(value = "topLevelInstitutionId", required = false)
        Integer topLevelInstitutionId,
        @RequestParam(defaultValue = "ONCE") RecurrenceType recurrence,
        @RequestHeader("Authorization") String bearerToken) {
        if (commissionIds.isEmpty()) {
            return;
        }
        reportingService.scheduleReportGeneration(timestamp, reportType, year, commissionIds, lang,
            topLevelInstitutionId, tokenUtil.extractUserIdFromToken(bearerToken), recurrence);
    }

    @GetMapping("/{commissionId}")
    @PreAuthorize("hasAuthority('SCHEDULE_REPORT_GENERATION')")
    public List<String> getAvailableReportsForCommission(@PathVariable Integer commissionId,
                                                         @RequestHeader(value = "Authorization")
                                                         String bearerToken) {
        return reportingService.getAvailableReportsForCommission(commissionId,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHEDULE_REPORT_GENERATION')")
    public List<ReportDTO> getAvailableReportsForUser(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return reportingService.getAvailableReportsForUser(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/download/{reportFileName}/{commissionId}")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> serveFile(HttpServletRequest request,
                                                           @PathVariable String reportFileName,
                                                           @PathVariable Integer commissionId,
                                                           @RequestHeader(value = "Authorization")
                                                           String bearerToken) throws IOException {
        if (!SessionUtil.isSessionValid(request, bearerToken) ||
            !SessionUtil.hasAnyRole(bearerToken,
                List.of(UserRole.ADMIN, UserRole.VICE_DEAN_FOR_SCIENCE))) {
            return ErrorResponseUtil.buildUnauthorisedStreamingResponse(request,
                "unauthorisedToViewDocumentMessage");
        }

        var file = reportingService.serveReportFile(reportFileName,
            tokenUtil.extractUserIdFromToken(bearerToken), commissionId);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"))
            .body(StreamingUtil.createStreamingBody(file));
    }
}
