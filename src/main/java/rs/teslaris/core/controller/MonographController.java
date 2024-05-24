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
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.service.interfaces.document.MonographService;

@RestController
@RequestMapping("/api/monograph")
@RequiredArgsConstructor
public class MonographController {

    private final MonographService monographService;


    @GetMapping("/{documentId}")
    private MonographDTO readMonograph(@PathVariable Integer documentId) {
        return monographService.readMonographById(documentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public MonographDTO createMonograph(@RequestBody @Valid MonographDTO monograph) {
        var savedMonograph = monographService.createMonograph(monograph, true);
        monograph.setId(savedMonograph.getId());
        return monograph;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editMonograph(@PathVariable Integer documentId,
                              @RequestBody @Valid MonographDTO monograph) {
        monographService.updateMonograph(documentId, monograph);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteMonograph(@PathVariable Integer documentId) {
        monographService.deleteMonograph(documentId);
    }
}
