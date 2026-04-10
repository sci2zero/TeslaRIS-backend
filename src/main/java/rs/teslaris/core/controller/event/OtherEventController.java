package rs.teslaris.core.controller.event;

import jakarta.validation.Valid;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.OtherEventService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/other-event")
@RequiredArgsConstructor
@Traceable
public class OtherEventController {

    private final OtherEventService otherEventService;

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{otherEventId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_OTHER_EVENTS')")
    public boolean canEditOtherEvent() {
        return true;
    }

    @GetMapping("/{otherEventId}/can-classify")
    @PreAuthorize("hasAuthority('EDIT_EVENT_ASSESSMENT_CLASSIFICATION') and hasAuthority('EDIT_EVENT_INDICATORS')")
    public boolean canClassifyOtherEvent() {
        return true;
    }

    @GetMapping
    public Page<OtherEventDTO> readAll(Pageable pageable) {
        return otherEventService.readAllOtherEvents(pageable);
    }

    @GetMapping("/import-search")
    Page<EventIndex> searchOtherEventsImport(
        @RequestParam("names") List<String> names,
        @RequestParam("dateFrom") String dateFrom,
        @RequestParam("dateTo") String dateTo) {
        StringUtil.sanitizeTokens(names);
        return otherEventService.searchOtherEventsForImport(names, dateFrom, dateTo);
    }

    @GetMapping("/{otherEventId}")
    public OtherEventDTO readOtherEvent(@PathVariable Integer otherEventId) {
        return otherEventService.readOtherEvent(otherEventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CREATE_OTHER_EVENTS', 'EDIT_OTHER_EVENTS')")
    @Idempotent
    public OtherEventDTO createOtherEvent(@RequestBody @Valid OtherEventDTO otherEventDTO,
                                          @RequestHeader(value = "Authorization", required = false)
                                          String bearerToken) {
        if (!tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.name()) &&
            !tokenUtil.extractUserRoleFromToken(bearerToken)
                .equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            otherEventDTO.setSerialEvent(false); // no one besides these roles can add serial events
        }

        var newOtherEvent = otherEventService.createOtherEvent(otherEventDTO, true);
        otherEventDTO.setId(newOtherEvent.getId());
        return otherEventDTO;
    }

    @PutMapping("/{otherEventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OTHER_EVENTS')")
    public void updateOtherEvent(@PathVariable Integer otherEventId,
                                 @RequestBody @Valid OtherEventDTO otherEventDTO) {
        otherEventService.updateOtherEvent(otherEventId, otherEventDTO);
    }

    @DeleteMapping("/{otherEventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OTHER_EVENTS')")
    public void deleteOtherEvent(@PathVariable Integer otherEventId) {
        otherEventService.deleteOtherEvent(otherEventId);
        deduplicationService.deleteSuggestion(otherEventId, EntityType.EVENT);
    }

    @DeleteMapping("/force/{otherEventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteOtherEvent(@PathVariable Integer otherEventId) {
        otherEventService.forceDeleteOtherEvent(otherEventId);
        deduplicationService.deleteSuggestion(otherEventId, EntityType.EVENT);
    }

    @PatchMapping("/{otherEventId}/reorder-contribution/{contributionId}")
    @PreAuthorize("hasAuthority('EDIT_OTHER_EVENTS')")
    void reorderOtherEventContributions(@PathVariable Integer otherEventId,
                                        @PathVariable Integer contributionId,
                                        @RequestBody ReorderContributionRequestDTO reorderRequest) {
        otherEventService.reorderOtherEventContributions(otherEventId, contributionId,
            reorderRequest.getOldContributionOrderNumber(),
            reorderRequest.getNewContributionOrderNumber());
    }

    @GetMapping("/identifier-usage/{otherEventId}")
    @PreAuthorize("hasAuthority('EDIT_OTHER_EVENTS')")
    public boolean checkIdentifierUsage(@PathVariable Integer otherEventId,
                                        @RequestParam String identifier) {
        return otherEventService.isIdentifierInUse(identifier, otherEventId);
    }
}
