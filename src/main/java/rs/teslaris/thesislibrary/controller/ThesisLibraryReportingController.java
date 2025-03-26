package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public List<ThesisReportCountsDTO> getReportCounts(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest) {
        return thesisLibraryReportingService.createThesisCountsReport(reportRequest);
    }

    @PostMapping("/defended")
    public Page<DocumentPublicationIndex> getDefendedThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchDefendedThesesInPeriod(reportRequest, pageable);
    }

    @PostMapping("/accepted")
    public Page<DocumentPublicationIndex> getAcceptedThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchAcceptedThesesInPeriod(reportRequest, pageable);
    }

    @PostMapping("/public-review")
    public Page<DocumentPublicationIndex> getPublicReviewThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchPublicReviewThesesInPeriod(reportRequest,
            pageable);
    }

    @PostMapping("/public-access")
    public Page<DocumentPublicationIndex> getPubliclyAvailableThesesForPeriod(
        @RequestBody @Valid ThesisReportRequestDTO reportRequest, Pageable pageable) {
        return thesisLibraryReportingService.fetchPubliclyAvailableThesesInPeriod(reportRequest,
            pageable);
    }
}
