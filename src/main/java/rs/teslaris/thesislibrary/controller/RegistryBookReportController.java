package rs.teslaris.thesislibrary.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookReportService;

@RestController
@RequestMapping("/api/registry-book/report")
@RequiredArgsConstructor
public class RegistryBookReportController {

    private final RegistryBookReportService registryBookReportService;

    private final JwtUtil tokenUtil;


    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    public void generateReport(@RequestParam LocalDate from,
                               @RequestParam LocalDate to,
                               @RequestParam Integer institutionId,
                               @RequestParam String lang) {
        registryBookReportService.generateReport(from, to, institutionId, lang);
    }

    @GetMapping("/list-reports")
    @PreAuthorize("hasAuthority('GENERATE_REG_BOOK_REPORT')")
    public List<String> listReports(@RequestHeader(value = "Authorization")
                                    String bearerToken) {
        return registryBookReportService.listAvailableReports(
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
