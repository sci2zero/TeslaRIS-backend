package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.thesislibrary.dto.ThesisReportCountsDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryReportingService;

@RestController
@RequestMapping("/api/thesis-library/report/")
@RequiredArgsConstructor
public class ThesisLibraryReportingController {

    private final ThesisLibraryReportingService thesisLibraryReportingService;

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
    @PreAuthorize("hasAuthority('PERFORM_THESIS_REPORT')")
    public ResponseEntity<InputStreamResource> generateThesisLibraryReportDocument(
        @PathVariable String lang, @RequestBody @Valid ThesisReportRequestDTO reportRequest) {
        var document = thesisLibraryReportingService.generatePhdLibraryReportDocument(reportRequest,
            lang.toUpperCase());

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.docx")
            .body(document);
    }
}
