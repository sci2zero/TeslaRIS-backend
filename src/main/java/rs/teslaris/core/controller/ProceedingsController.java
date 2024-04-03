package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;

@RestController
@RequestMapping("/api/proceedings")
@RequiredArgsConstructor
public class ProceedingsController {

    private final ProceedingsService proceedingsService;


    @GetMapping("/{publicationId}")
    public ProceedingsResponseDTO readProceedings(@PathVariable Integer publicationId) {
        return proceedingsService.readProceedingsById(publicationId);
    }

    @GetMapping("/for-event/{eventId}")
    public List<ProceedingsResponseDTO> readProceedingsForEvent(@PathVariable Integer eventId) {
        return proceedingsService.readProceedingsForEventId(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public ProceedingsDTO createProceedings(@RequestBody @Valid ProceedingsDTO proceedings) {
        var savedProceedings = proceedingsService.createProceedings(proceedings, true);
        proceedings.setId(savedProceedings.getId());
        return proceedings;
    }

    @PutMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editProceedings(@PathVariable Integer publicationId,
                                @RequestBody @Valid ProceedingsDTO proceedings) {
        proceedingsService.updateProceedings(publicationId, proceedings);
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteProceedings(@PathVariable Integer publicationId) {
        proceedingsService.deleteProceedings(publicationId);
    }
}
