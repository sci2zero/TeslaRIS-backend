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
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.service.JournalPublicationService;

@RestController
@RequestMapping("api/journal-publication")
@RequiredArgsConstructor
public class JournalPublicationController {

    private final JournalPublicationService journalPublicationService;


    @GetMapping("/{publicationId}")
    public JournalPublicationResponseDTO readJournalPublication(
        @PathVariable Integer publicationId) {
        return journalPublicationService.readJournalPublicationById(publicationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    public JournalPublicationDTO createJournalPublication(
        @RequestBody @Valid JournalPublicationDTO journalPublication) {
        var savedJournalPublication =
            journalPublicationService.createJournalPublication(journalPublication);
        journalPublication.setId(savedJournalPublication.getId());
        return journalPublication;
    }

    @PutMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editJournalPublication(@PathVariable Integer publicationId,
                                       @RequestBody
                                       @Valid JournalPublicationDTO journalPublication) {
        journalPublicationService.editJournalPublication(publicationId, journalPublication);
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteJournalPublication(@PathVariable Integer publicationId) {
        journalPublicationService.deleteJournalPublication(publicationId);
    }
}
