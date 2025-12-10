package rs.teslaris.exporter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import rs.teslaris.exporter.service.interfaces.RoCrateExportService;

@RestController
@RequestMapping("/api/ro-crate")
@RequiredArgsConstructor
public class RoCrateController {

    private final RoCrateExportService roCrateExportService;


    @GetMapping("/document/{id}")
    public ResponseEntity<StreamingResponseBody> downloadRoCrate(@PathVariable Integer id) {

        StreamingResponseBody body =
            outputStream -> roCrateExportService.createRoCrateZip(id, outputStream);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"ro-crate-" + id + ".zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(body);
    }
}
