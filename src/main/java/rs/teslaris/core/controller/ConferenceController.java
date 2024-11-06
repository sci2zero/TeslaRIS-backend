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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.IndexType;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/conference")
@RequiredArgsConstructor
public class ConferenceController {

    private final ConferenceService conferenceService;

    private final DeduplicationService deduplicationService;


    @GetMapping("/{conferenceId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_CONFERENCES')")
    public boolean canEditConference() {
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
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return conferenceService.searchConferences(tokens, pageable, returnOnlyNonSerialEvents,
            returnOnlySerialEvents);
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
        deduplicationService.deleteSuggestion(conferenceId, IndexType.EVENT);
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
}
