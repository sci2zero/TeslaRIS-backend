package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("/api/proceedings")
@RequiredArgsConstructor
@Traceable
public class ProceedingsController {

    private final ProceedingsService proceedingsService;

    private final DeduplicationService deduplicationService;


    @GetMapping("/{documentId}")
    public ResponseEntity<ProceedingsResponseDTO> readProceedings(
        @PathVariable Integer documentId) {
        var dto = proceedingsService.readProceedingsById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/proceedings"))
            .body(dto);
    }

    @GetMapping("/for-event/{eventId}")
    public List<ProceedingsResponseDTO> readProceedingsForEvent(@PathVariable Integer eventId) {
        return proceedingsService.readProceedingsForEventId(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck(value = "CREATE")
    @Idempotent
    public ProceedingsDTO createProceedings(@RequestBody @Valid ProceedingsDTO proceedings) {
        var savedProceedings = proceedingsService.createProceedings(proceedings, true);
        proceedings.setId(savedProceedings.getId());
        return proceedings;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editProceedings(@PathVariable Integer documentId,
                                @RequestBody @Valid ProceedingsDTO proceedings) {
        proceedingsService.updateProceedings(documentId, proceedings);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteProceedings(@PathVariable Integer documentId) {
        proceedingsService.deleteProceedings(documentId);
        deduplicationService.deleteSuggestion(documentId, EntityType.PUBLICATION);
    }

    @DeleteMapping("/force/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteProceedings(@PathVariable Integer documentId) {
        proceedingsService.forceDeleteProceedings(documentId);
        deduplicationService.deleteSuggestion(documentId, EntityType.PUBLICATION);
    }

    @GetMapping("/identifier-usage/{documentId}")
    @PublicationEditCheck
    public boolean checkIdentifierUsage(@PathVariable Integer documentId,
                                        @RequestParam String identifier) {
        return proceedingsService.isIdentifierInUse(identifier, documentId);
    }
}
