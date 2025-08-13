package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
@Traceable
public class JournalController {

    private final JournalService journalService;

    private final DeduplicationService deduplicationService;

    private final UserService userService;

    private final JwtUtil tokenUtil;

    private final EmailUtil emailUtil;


    @GetMapping("/{journalId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public boolean canEditJournal() {
        return true;
    }

    @GetMapping("/{journalId}/can-classify")
    @PreAuthorize("hasAuthority('EDIT_PUB_SERIES_ASSESSMENT_CLASSIFICATION') and hasAuthority('EDIT_PUB_SERIES_INDICATORS')")
    public boolean canClassifyJournal() {
        return true;
    }

    @GetMapping
    public Page<JournalResponseDTO> readAll(Pageable pageable) {
        return journalService.readAllJournals(pageable);
    }

    @GetMapping("/simple-search")
    Page<JournalIndex> searchJournals(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(value = "institutionId", required = false) Integer institutionId,
        @RequestParam(value = "commissionId", required = false) Integer commissionId,
        @RequestParam(value = "unclassified", defaultValue = "false") Boolean unclassified,
        @RequestHeader(value = "Authorization", defaultValue = "") String bearerToken,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);

        if (!bearerToken.isEmpty()) {
            if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.name())) {
                return journalService.searchJournals(tokens, pageable, institutionId,
                    unclassified ? commissionId : null);
            } else if (tokenUtil.extractUserRoleFromToken(bearerToken)
                .equals(UserRole.COMMISSION.name())) {
                var userId = tokenUtil.extractUserIdFromToken(bearerToken);

                return journalService.searchJournals(tokens, pageable, institutionId,
                    unclassified ? userService.getUserCommissionId(userId) : null);
            }
        }

        return journalService.searchJournals(tokens, pageable, institutionId, null);
    }

    @GetMapping("/{journalId}")
    public JournalResponseDTO readJournal(@PathVariable Integer journalId) {
        return journalService.readJournal(journalId);
    }

    @GetMapping("/identifier")
    public JournalIndex readJournal(@RequestParam String eIssn, @RequestParam String printIssn,
                                    @RequestParam String openAlexId) {
        return journalService.readJournalByIdentifiers(eIssn, printIssn, openAlexId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CREATE_JOURNAL')")
    @Idempotent
    public PublicationSeriesDTO createJournal(@RequestBody @Valid PublicationSeriesDTO journalDTO) {
        var savedJournal = journalService.createJournal(journalDTO, true);

        savedJournal.getTitle().stream().findFirst().ifPresent(mc -> {
            emailUtil.notifyInstitutionalEditor(savedJournal.getId(), mc.getContent(), "journal");
        });

        journalDTO.setId(savedJournal.getId());
        return journalDTO;
    }

    @PostMapping("/basic")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    @Idempotent
    public JournalBasicAdditionDTO createJournal(
        @RequestBody @Valid JournalBasicAdditionDTO journalDTO) {
        var savedJournal = journalService.createJournal(journalDTO);

        savedJournal.getTitle().stream().findFirst().ifPresent(mc -> {
            emailUtil.notifyInstitutionalEditor(savedJournal.getId(), mc.getContent(), "journal");
        });

        journalDTO.setId(savedJournal.getId());
        return journalDTO;
    }

    @PutMapping("/{journalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public void updateJournal(@RequestBody @Valid PublicationSeriesDTO journalDTO,
                              @PathVariable Integer journalId) {
        journalService.updateJournal(journalId, journalDTO);
    }

    @DeleteMapping("/{journalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public void deleteJournal(@PathVariable Integer journalId) {
        journalService.deleteJournal(journalId);
        deduplicationService.deleteSuggestion(journalId, EntityType.JOURNAL);
    }

    @DeleteMapping("/force/{journalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteJournal(@PathVariable Integer journalId) {
        journalService.forceDeleteJournal(journalId);
        deduplicationService.deleteSuggestion(journalId, EntityType.JOURNAL);
    }

    @GetMapping("/identifier-usage/{journalId}")
    @PreAuthorize("hasAuthority('EDIT_PUBLICATION_SERIES')")
    public boolean checkIdentifierUsage(@PathVariable Integer journalId,
                                        @RequestParam String identifier) {
        return journalService.isIdentifierInUse(identifier, journalId);
    }
}
