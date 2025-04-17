package rs.teslaris.thesislibrary.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookReportService;

@RestController
@RequestMapping("/api/registry-book/report")
@RequiredArgsConstructor
public class RegistryBookReportController {

    private final RegistryBookReportService registryBookReportService;

    private final JwtUtil tokenUtil;


    @PostMapping("/schedule-generation")
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String generateReport(@RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to,
                                 @RequestParam Integer institutionId,
                                 @RequestParam String lang,
                                 @RequestHeader(value = "Authorization")
                                 String bearerToken) {
        return registryBookReportService.scheduleReportGeneration(
            Objects.nonNull(from) ? from : LocalDate.of(1000, 1, 1),
            Objects.nonNull(to) ? to : LocalDate.now(), institutionId, lang,
            tokenUtil.extractUserIdFromToken(bearerToken));
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
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    @ResponseBody
    public ResponseEntity<Object> serveFile(@PathVariable String reportFileName,
                                            @RequestHeader(value = "Authorization")
                                            String bearerToken) throws IOException {
        var file = registryBookReportService.serveReportFile(reportFileName,
            tokenUtil.extractUserIdFromToken(bearerToken));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, file.headers().get("Content-Disposition"))
            .header(HttpHeaders.CONTENT_TYPE, file.headers().get("Content-Type"))
            .body(new InputStreamResource(file));
    }
}
