package rs.teslaris.core.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.service.DocumentPublicationService;

@RestController
@RequestMapping("api/journal-publication")
@RequiredArgsConstructor
public class JournalPublicationController {

    private final DocumentPublicationService documentPublicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JournalPublicationDTO createJournalPublication(
        @RequestBody @Valid JournalPublicationDTO journalPublication) {
        var savedJournalPublication =
            documentPublicationService.createJournalPublication(journalPublication);
        journalPublication.setId(savedJournalPublication.getId());
        return journalPublication;
    }

    @PutMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editJournalPublication(@PathVariable Integer publicationId,
                                       @RequestBody
                                       @Valid JournalPublicationDTO journalPublication) {
        documentPublicationService.editJournalPublication(publicationId, journalPublication);
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJournalPublication(@PathVariable Integer publicationId) {
        documentPublicationService.deleteJournalPublication(publicationId);
    }
}
