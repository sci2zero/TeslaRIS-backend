package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("api/journal-publication")
@RequiredArgsConstructor
@Traceable
public class JournalPublicationController {

    private final JournalPublicationService journalPublicationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{documentId}")
    public ResponseEntity<JournalPublicationResponseDTO> readJournalPublication(
        @PathVariable Integer documentId) {
        var dto = journalPublicationService.readJournalPublicationById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/journal-publication"))
            .body(dto);
    }

    @GetMapping("/journal/{journalId}/my-publications")
    @PreAuthorize("hasAuthority('LIST_MY_JOURNAL_PUBLICATIONS')")
    public List<DocumentPublicationIndex> findMyPublicationsInJournal(
        @PathVariable Integer journalId, @RequestHeader("Authorization") String bearerToken) {
        return journalPublicationService.findMyPublicationsInJournal(journalId,
            userService.getPersonIdForUser(
                tokenUtil.extractUserIdFromToken(bearerToken)));
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

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editJournalPublication(@PathVariable Integer documentId,
                                       @RequestBody
                                       @Valid JournalPublicationDTO journalPublication) {
        journalPublicationService.editJournalPublication(documentId, journalPublication);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteJournalPublication(@PathVariable Integer documentId) {
        journalPublicationService.deleteJournalPublication(documentId);
    }

    @PatchMapping("/transfer/{documentId}/{journalId}")
    @PreAuthorize("hasAuthority('CHANGE_PUBLICATION_TYPE')")
    @PublicationEditCheck
    public Integer transferProceedingsPublicationToJournal(@PathVariable Integer documentId,
                                                           @PathVariable Integer journalId) {
        return journalPublicationService.transferProceedingsPublicationToJournal(documentId,
            journalId);
    }
}
