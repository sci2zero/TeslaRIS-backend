package rs.teslaris.thesislibrary.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.files.StreamingUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookReportService;

@RestController
@RequestMapping("/api/registry-book/report")
@RequiredArgsConstructor
@Traceable
public class RegistryBookReportController {

    private final RegistryBookReportService registryBookReportService;

    private final JwtUtil tokenUtil;


    @PostMapping("/schedule-generation")
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String generateReport(@RequestParam(required = false) RelativeDateDTO from,
                                 @RequestParam(required = false) RelativeDateDTO to,
                                 @RequestParam(required = false, defaultValue = "")
                                 String authorName,
                                 @RequestParam(required = false, defaultValue = "")
                                 String authorTitle,
                                 @RequestParam Integer institutionId,
                                 @RequestParam String lang,
                                 @RequestParam(defaultValue = "ONCE") RecurrenceType recurrence,
                                 @RequestHeader(value = "Authorization")
                                 String bearerToken) {
        return registryBookReportService.scheduleReportGeneration(
            Objects.nonNull(from) ? from : RelativeDateDTO.of(1000, 1, 1),
            Objects.nonNull(to) ? to : RelativeDateDTO.now(), institutionId, lang,
            tokenUtil.extractUserIdFromToken(bearerToken), authorName, authorTitle, recurrence);
    }

    @GetMapping("/list-reports")
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    public List<String> listReports(@RequestHeader(value = "Authorization")
                                    String bearerToken) {
        return registryBookReportService.listAvailableReports(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @DeleteMapping("/{reportFileName}")
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    public void deleteReportFile(@PathVariable String reportFileName,
                                 @RequestHeader(value = "Authorization")
                                 String bearerToken) {
        registryBookReportService.deleteReportFile(reportFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/download/{reportFileName}")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> serveFile(HttpServletRequest request,
                                                           @PathVariable String reportFileName,
                                                           @RequestHeader(value = "Authorization")
                                                           String bearerToken) throws IOException {
        if (!SessionUtil.isSessionValid(request, bearerToken) ||
            !SessionUtil.hasAnyRole(bearerToken,
                List.of(UserRole.ADMIN, UserRole.PROMOTION_REGISTRY_ADMINISTRATOR))) {
            return ErrorResponseUtil.buildUnauthorisedStreamingResponse(request,
                "unauthorisedToViewDocumentMessage");
        }

        var file = registryBookReportService.serveReportFile(reportFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"))
            .header(HttpHeaders.CONTENT_LENGTH, file.headers().get("Content-Length"))
            .body(StreamingUtil.createStreamingBody(file));
    }
}
