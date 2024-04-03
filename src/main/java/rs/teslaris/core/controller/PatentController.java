package rs.teslaris.core.controller;

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
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.service.interfaces.document.PatentService;

@RestController
@RequestMapping("api/patent")
@RequiredArgsConstructor
public class PatentController {

    private final PatentService patentService;

    @GetMapping("/{documentId}")
    public PatentDTO readPatent(
        @PathVariable Integer documentId) {
        return patentService.readPatentById(documentId);
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
