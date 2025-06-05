package rs.teslaris.exporter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.exporter.service.interfaces.CommonExportService;

@RestController
@RequestMapping("/api/common-export")
@RequiredArgsConstructor
@Traceable
public class CommonExportController {

    private final CommonExportService commonExportService;

    @GetMapping(value = "/OAIHandlerOpenAIRECRIS", produces = "application/xml")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PREPARE_EXPORT_DATA')")
    public void handleOAIOpenAIRECRIS(@RequestParam String set) {
        switch (set) {
            case "org_units":
                commonExportService.exportOrganisationUnitsToCommonModel();
                break;
            case "persons":
                commonExportService.exportPersonsToCommonModel();
                break;
            case "events":
                commonExportService.exportConferencesToCommonModel();
                break;
            case "documents":
                commonExportService.exportDocumentsToCommonModel();
                break;
        }
    }
}
