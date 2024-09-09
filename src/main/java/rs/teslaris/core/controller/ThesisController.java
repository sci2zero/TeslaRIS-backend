package rs.teslaris.core.controller;

import jakarta.validation.Valid;
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
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.service.interfaces.document.ThesisService;

@RestController
@RequestMapping("/api/thesis")
@RequiredArgsConstructor
public class ThesisController {

    private final ThesisService thesisService;

    @GetMapping("/{documentId}")
    public ThesisResponseDTO readThesis(
        @PathVariable Integer documentId) {
        return thesisService.readThesisById(documentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public ThesisDTO createThesis(@RequestBody @Valid ThesisDTO thesis) {
        var savedThesis = thesisService.createThesis(thesis, true);
        thesis.setId(savedThesis.getId());
        return thesis;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editThesis(@PathVariable Integer documentId,
                           @RequestBody @Valid ThesisDTO thesis) {
        thesisService.editThesis(documentId, thesis);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteThesis(@PathVariable Integer documentId) {
        thesisService.deleteThesis(documentId);
    }
}
