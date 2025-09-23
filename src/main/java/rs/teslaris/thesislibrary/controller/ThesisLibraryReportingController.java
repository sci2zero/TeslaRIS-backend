package rs.teslaris.thesislibrary.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.util.exceptionhandling.ErrorResponseUtil;
import rs.teslaris.core.util.files.StreamingUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.thesislibrary.dto.ThesisReportCountsDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryReportingService;

@RestController
@RequestMapping("/api/thesis-library/report/")
@RequiredArgsConstructor
@Traceable
public class ThesisLibraryReportingController {

    private final ThesisLibraryReportingService thesisLibraryReportingService;

    private final JwtUtil tokenUtil;


    @PostMapping("/counts")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_REPORT')")
    public List<ThesisReportCountsDTO> getReportCounts(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest) {
        return thesisLibraryReportingService.createThesisCountsReport(reportRequest);
    }

    @PostMapping("/defended")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_REPORT')")
    public Page<DocumentPublicationIndex> getDefendedThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchDefendedThesesInPeriod(reportRequest, pageable);
    }

    @PostMapping("/accepted")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_REPORT')")
    public Page<DocumentPublicationIndex> getAcceptedThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchAcceptedThesesInPeriod(reportRequest, pageable);
    }

    @PostMapping("/public-review")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_REPORT')")
    public Page<DocumentPublicationIndex> getPublicReviewThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchPublicReviewThesesInPeriod(reportRequest,
            pageable);
    }

    @PostMapping("/public-access")
    @PreAuthorize("hasAuthority('PERFORM_THESIS_REPORT')")
    public Page<DocumentPublicationIndex> getPubliclyAvailableThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchPubliclyAvailableThesesInPeriod(reportRequest,
            pageable);
    }

    @PostMapping("/download/{lang}")
    public ResponseEntity<StreamingResponseBody> generateThesisLibraryReportDocument(
        HttpServletRequest request, @PathVariable String lang,
        @RequestBody @Valid ThesisReportRequestDTO reportRequest,
        @RequestHeader(value = "Authorization") String bearerToken) throws IOException {
        if (!SessionUtil.isSessionValid(request, bearerToken) ||
            !SessionUtil.hasAnyRole(bearerToken,
                List.of(UserRole.ADMIN, UserRole.HEAD_OF_LIBRARY))) {
            return ErrorResponseUtil.buildUnauthorisedStreamingResponse(request,
                "unauthorisedToViewDocumentMessage");
        }

        var document = thesisLibraryReportingService.generatePhdLibraryReportDocument(reportRequest,
            lang.toUpperCase());

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.docx")
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(document.b))
            .body(StreamingUtil.createStreamingBody(document.a.getInputStream()));
    }
}
