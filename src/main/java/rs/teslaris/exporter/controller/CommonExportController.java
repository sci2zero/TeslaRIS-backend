package rs.teslaris.exporter.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.service.interfaces.CommonExportService;

@RestController
@RequestMapping("/api/common-export")
@RequiredArgsConstructor
@Traceable
public class CommonExportController {

    private final CommonExportService commonExportService;


    @PostMapping("/prepare-for-export")
    @PreAuthorize("hasAuthority('PREPARE_EXPORT_DATA')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void handleOAIOpenAIRECRIS(@RequestParam String set, @RequestParam boolean allTime,
                                      @RequestParam(required = false)
                                      List<ExportPublicationType> exportTypes) {
        switch (set) {
            case "org_units":
                commonExportService.exportOrganisationUnitsToCommonModel(allTime);
                break;
            case "persons":
                commonExportService.exportPersonsToCommonModel(allTime);
                break;
            case "events":
                commonExportService.exportConferencesToCommonModel(allTime);
                break;
            case "documents":
                commonExportService.exportDocumentsToCommonModel(allTime, exportTypes);
                break;
        }
    }
}
