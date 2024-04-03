package rs.teslaris.core.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("api/journal-publication")
@RequiredArgsConstructor
public class JournalPublicationController {

    private final JournalPublicationService journalPublicationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{publicationId}")
    public JournalPublicationResponseDTO readJournalPublication(
        @PathVariable Integer publicationId) {
        return journalPublicationService.readJournalPublicationById(publicationId);
    }

    @GetMapping("/journal/{journalId}/my-publications")
    @PreAuthorize("hasAuthority('LIST_MY_JOURNAL_PUBLICATIONS')")
    public List<DocumentPublicationIndex> findMyPublicationsInJournal(
        @PathVariable Integer journalId, @RequestHeader("Authorization") String bearerToken) {
        return journalPublicationService.findMyPublicationsInJournal(journalId,
            userService.getPersonIdForUser(
                tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1])));
    }

    @GetMapping("/journal/{journalId}")
    public Page<DocumentPublicationIndex> findPublicationsInJournal(
        @PathVariable Integer journalId, Pageable pageable) {
        return journalPublicationService.findPublicationsInJournal(journalId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public JournalPublicationDTO createJournalPublication(
        @RequestBody @Valid JournalPublicationDTO journalPublication) {
        var savedJournalPublication =
            journalPublicationService.createJournalPublication(journalPublication, true);
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
