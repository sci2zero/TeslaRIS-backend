package rs.teslaris.core.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.service.interfaces.document.JournalService;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public Page<JournalResponseDTO> readAll(Pageable pageable) {
        return journalService.readAllJournals(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_JOURNALS')")
    @Idempotent
    public JournalDTO createJournal(@RequestBody @Valid JournalDTO journalDTO) {
        var createdJournal = journalService.createJournal(journalDTO);
        journalDTO.setId(createdJournal.getId());
        return journalDTO;
    }

    @PutMapping("/{journalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_JOURNALS')")
    public void updateJournal(@RequestBody @Valid JournalDTO journalDTO,
                              @PathVariable Integer journalId) {
        journalService.updateJournal(journalDTO, journalId);
    }

    @DeleteMapping("/{journalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_JOURNALS')")
    public void deleteJournal(@PathVariable Integer journalId) {
        journalService.deleteJournal(journalId);
    }
}