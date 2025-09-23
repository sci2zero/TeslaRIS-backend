package rs.teslaris.importer.controller;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.document.EventService;

@RestController
@RequestMapping("/api/extra-migration")
@RequiredArgsConstructor
public class ExtraMigrationController {

    private final EventService eventService;

    @PatchMapping("/event")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enrichEventInformationFromExternalSource(@RequestParam Integer oldId,
                                                         @RequestParam LocalDate dateFrom,
                                                         @RequestParam LocalDate dateTo) {
        eventService.enrichEventInformationFromExternalSource(oldId, dateFrom, dateTo);
    }
}
