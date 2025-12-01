package rs.teslaris.thesislibrary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookDraftService;

@RestController
@RequestMapping("/api/registry-book-draft")
@RequiredArgsConstructor
@Traceable
public class RegistryBookDraftController {

    private final RegistryBookDraftService registryBookDraftService;


    @PatchMapping("/{documentId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Idempotent
    public void createRegistryBookEntry(
        @RequestBody RegistryBookEntryDTO registryBookEntryDTO,
        @PathVariable Integer documentId) {
        registryBookDraftService.saveRegistryBookEntryDraft(registryBookEntryDTO, documentId);
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    @PublicationEditCheck("THESIS")
    public RegistryBookEntryDTO getEntryPrePopulatedData(@PathVariable Integer documentId) {
        return registryBookDraftService.fetchRegistryBookEntryDraft(documentId);
    }
}
