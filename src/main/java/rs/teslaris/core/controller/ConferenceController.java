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
import org.springframework.web.bind.annotation.PatchMapping;
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
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/conference")
@RequiredArgsConstructor
public class ConferenceController {

    private final ConferenceService conferenceService;

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{conferenceId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public boolean canEditConference() {
        return true;
    }

    @GetMapping("/{conferenceId}/can-classify")
    @PreAuthorize("hasAuthority('EDIT_EVENT_ASSESSMENT_CLASSIFICATION') and hasAuthority('EDIT_EVENT_INDICATORS')")
    public boolean canClassifyConference() {
        return true;
    }

    @GetMapping
    public Page<ConferenceDTO> readAll(Pageable pageable) {
        return conferenceService.readAllConferences(pageable);
    }

    @GetMapping("/simple-search")
    Page<EventIndex> searchConferences(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam("returnOnlyNonSerialEvents")
        @NotNull(message = "You have to provide search range.") Boolean returnOnlyNonSerialEvents,
        @RequestParam("returnOnlySerialEvents")
        @NotNull(message = "You have to provide search range.") Boolean returnOnlySerialEvents,
        @RequestParam(value = "forMyInstitution", defaultValue = "false") Boolean forMyInstitution,
        @RequestHeader("Authorization") String bearerToken,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);

        if (forMyInstitution &&
            tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name())) {
            return conferenceService.searchConferences(tokens, pageable, returnOnlyNonSerialEvents,
                returnOnlySerialEvents, userService.getUserOrganisationUnitId(
                    tokenUtil.extractUserIdFromToken(bearerToken)));
        }

        return conferenceService.searchConferences(tokens, pageable, returnOnlyNonSerialEvents,
            returnOnlySerialEvents, null);
    }

    @GetMapping("/import-search")
    Page<EventIndex> searchConferencesImport(
        @RequestParam("names") List<String> names,
        @RequestParam("dateFrom") String dateFrom,
        @RequestParam("dateTo") String dateTo) {
        StringUtil.sanitizeTokens(names);
        return conferenceService.searchConferencesForImport(names, dateFrom, dateTo);
    }

    @GetMapping("/{conferenceId}")
    public ConferenceDTO readConference(@PathVariable Integer conferenceId) {
        return conferenceService.readConference(conferenceId);
    }

    @GetMapping("/old-id/{oldConferenceId}")
    public ConferenceDTO readConferenceByOldId(@PathVariable Integer oldConferenceId) {
        return conferenceService.readConferenceByOldId(oldConferenceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    @Idempotent
    public ConferenceDTO createConference(@RequestBody @Valid ConferenceDTO conferenceDTO) {
        var newConference = conferenceService.createConference(conferenceDTO, true);
        conferenceDTO.setId(newConference.getId());
        return conferenceDTO;
    }

    @PostMapping("/basic")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    @Idempotent
    public ConferenceBasicAdditionDTO createConferenceBasic(
        @RequestBody @Valid ConferenceBasicAdditionDTO conferenceDTO) {
        var newConference = conferenceService.createConference(conferenceDTO);
        conferenceDTO.setId(newConference.getId());
        return conferenceDTO;
    }

    @PutMapping("/{conferenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public void updateConference(@PathVariable Integer conferenceId,
                                 @RequestBody @Valid ConferenceDTO conferenceDTO) {
        conferenceService.updateConference(conferenceId, conferenceDTO);
    }

    @DeleteMapping("/{conferenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public void deleteConference(@PathVariable Integer conferenceId) {
        conferenceService.deleteConference(conferenceId);
        deduplicationService.deleteSuggestion(conferenceId, EntityType.EVENT);
    }

    @DeleteMapping("/force/{conferenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteConference(@PathVariable Integer conferenceId) {
        conferenceService.forceDeleteConference(conferenceId);
        deduplicationService.deleteSuggestion(conferenceId, EntityType.EVENT);
    }

    @PatchMapping("/{conferenceId}/reorder-contribution/{contributionId}")
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    void reorderEventContributions(@PathVariable Integer conferenceId,
                                   @PathVariable Integer contributionId,
                                   @RequestBody ReorderContributionRequestDTO reorderRequest) {
        conferenceService.reorderConferenceContributions(conferenceId, contributionId,
            reorderRequest.getOldContributionOrderNumber(),
            reorderRequest.getNewContributionOrderNumber());
    }

    @GetMapping("/identifier-usage/{conferenceId}/{identifier}")
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public boolean checkIdentifierUsage(@PathVariable Integer conferenceId,
                                        @PathVariable String identifier) {
        return conferenceService.isIdentifierInUse(identifier, conferenceId);
    }
}
