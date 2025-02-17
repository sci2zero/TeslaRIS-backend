package rs.teslaris.core.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.assessment.model.ReportType;
import rs.teslaris.core.assessment.service.interfaces.ReportingService;

@RestController
@RequestMapping("/api/assessment/report")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @PostMapping("/generate/{reportType}/{commissionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void generateReport(@PathVariable ReportType reportType,
                               @PathVariable Integer commissionId, @RequestParam Integer year) {
        reportingService.generateReport(reportType, year, commissionId);
    }
}
