package rs.teslaris.thesislibrary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.thesislibrary.dto.ThesisPublicReviewResponseDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryDissertationReportService;

@RestController
@RequestMapping("/api/thesis-library/dissertation-report")
@RequiredArgsConstructor
public class ThesisLibraryDissertationReportController {

    private final ThesisLibraryDissertationReportService thesisLibraryDissertationReportService;


    @GetMapping
    public Page<ThesisPublicReviewResponseDTO> fetchPublicReviewDissertations(
        @RequestParam(value = "institutionId", required = false) Integer institutionId,
        @RequestParam(value = "year", required = false) Integer year,
        @RequestParam(value = "notDefendedOnly", required = false) Boolean notDefendedOnly,
        Pageable pageable) {
        return thesisLibraryDissertationReportService.fetchPublicReviewDissertations(institutionId,
            year, notDefendedOnly, pageable);
    }
}
