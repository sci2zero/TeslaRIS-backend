package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.util.signposting.FairSignposting;

@RestController
@RequestMapping("api/patent")
@RequiredArgsConstructor
@Traceable
public class PatentController {

    private final PatentService patentService;

    @GetMapping("/{documentId}")
    public ResponseEntity<PatentDTO> readPatent(
        @PathVariable Integer documentId) {
        var dto = patentService.readPatentById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignposting.constructHeaders(dto, "/api/patent"))
            .body(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public PatentDTO createPatent(@RequestBody @Valid PatentDTO patent) {
        var savedPatent = patentService.createPatent(patent, true);
        patent.setId(savedPatent.getId());
        return patent;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editPatent(@PathVariable Integer documentId,
                           @RequestBody @Valid PatentDTO patent) {
        patentService.editPatent(documentId, patent);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deletePatent(@PathVariable Integer documentId) {
        patentService.deletePatent(documentId);
    }
}
