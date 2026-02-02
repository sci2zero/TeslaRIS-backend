package rs.teslaris.core.controller.event;

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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.ExhibitionService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/exhibition")
@RequiredArgsConstructor
@Traceable
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final EmailUtil emailUtil;


    @GetMapping("/{exhibitionId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_EXHIBITIONS')")
    public boolean canEditExhibition() {
        return true;
    }

    @GetMapping("/{exhibitionId}/can-classify")
    @PreAuthorize("hasAuthority('EDIT_EVENT_ASSESSMENT_CLASSIFICATION') and hasAuthority('EDIT_EVENT_INDICATORS')")
    public boolean canClassifyExhibition() {
        return true;
    }

    @GetMapping
    public Page<ExhibitionDTO> readAll(Pageable pageable) {
        return exhibitionService.readAllExhibitions(pageable);
    }

    @GetMapping("/simple-search")
    Page<EventIndex> searchExhibitions(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam("returnOnlyNonSerialEvents")
        @NotNull(message = "You have to provide search range.") Boolean returnOnlyNonSerialEvents,
        @RequestParam("returnOnlySerialEvents")
        @NotNull(message = "You have to provide search range.") Boolean returnOnlySerialEvents,
        @RequestParam(value = "forMyInstitution", defaultValue = "false") Boolean forMyInstitution,
        @RequestParam(value = "commissionId", required = false) Integer commissionId,
        @RequestParam(value = "unclassified", defaultValue = "false") Boolean unclassified,
        @RequestParam(value = "emptyEventsOnly", defaultValue = "false") Boolean emptyEventsOnly,
        @RequestHeader(value = "Authorization", defaultValue = "") String bearerToken,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);

        if (!bearerToken.isEmpty()) {
            if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.name())) {
                return exhibitionService.searchExhibitions(tokens, pageable,
                    returnOnlyNonSerialEvents,
                    returnOnlySerialEvents, null,
                    unclassified ? commissionId : null, emptyEventsOnly);
            } else if (tokenUtil.extractUserRoleFromToken(bearerToken)
                .equals(UserRole.COMMISSION.name())) {
                var userId = tokenUtil.extractUserIdFromToken(bearerToken);

                return exhibitionService.searchExhibitions(tokens, pageable,
                    returnOnlyNonSerialEvents,
                    returnOnlySerialEvents,
                    forMyInstitution ? userService.getUserOrganisationUnitId(userId) : null,
                    unclassified ? userService.getUserCommissionId(userId) : null, false);
            }
        }

        return exhibitionService.searchExhibitions(tokens, pageable, returnOnlyNonSerialEvents,
            returnOnlySerialEvents, null, null, false);
    }

    @GetMapping("/import-search")
    Page<EventIndex> searchExhibitionsImport(
        @RequestParam("names") List<String> names,
        @RequestParam("dateFrom") String dateFrom,
        @RequestParam("dateTo") String dateTo) {
        StringUtil.sanitizeTokens(names);
        return exhibitionService.searchExhibitionsForImport(names, dateFrom, dateTo);
    }

    @GetMapping("/{exhibitionId}")
    public ExhibitionDTO readExhibition(@PathVariable Integer exhibitionId) {
        return exhibitionService.readExhibition(exhibitionId);
    }

    @GetMapping("/old-id/{oldExhibitionId}")
    public ExhibitionDTO readExhibitionByOldId(@PathVariable Integer oldExhibitionId) {
        return exhibitionService.readExhibitionByOldId(oldExhibitionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CREATE_EXHIBITIONS', 'EDIT_EXHIBITIONS')")
    @Idempotent
    public ExhibitionDTO createExhibition(@RequestBody @Valid ExhibitionDTO exhibitionDTO,
                                          @RequestHeader(value = "Authorization", required = false)
                                          String bearerToken) {
        if (!tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.name()) &&
            !tokenUtil.extractUserRoleFromToken(bearerToken)
                .equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            exhibitionDTO.setSerialEvent(false); // no one besides these roles can add serial events
        }

        var newExhibition = exhibitionService.createExhibition(exhibitionDTO, true);
        exhibitionDTO.setId(newExhibition.getId());
        return exhibitionDTO;
    }

    @PutMapping("/{exhibitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_EXHIBITIONS')")
    public void updateExhibition(@PathVariable Integer exhibitionId,
                                 @RequestBody @Valid ExhibitionDTO exhibitionDTO) {
        exhibitionService.updateExhibition(exhibitionId, exhibitionDTO);
    }

    @DeleteMapping("/{exhibitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_EXHIBITIONS')")
    public void deleteExhibition(@PathVariable Integer exhibitionId) {
        exhibitionService.deleteExhibition(exhibitionId);
        deduplicationService.deleteSuggestion(exhibitionId, EntityType.EVENT);
    }

    @DeleteMapping("/force/{exhibitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteExhibition(@PathVariable Integer exhibitionId) {
        exhibitionService.forceDeleteExhibition(exhibitionId);
        deduplicationService.deleteSuggestion(exhibitionId, EntityType.EVENT);
    }

    @PatchMapping("/{exhibitionId}/reorder-contribution/{contributionId}")
    @PreAuthorize("hasAuthority('EDIT_EXHIBITIONS')")
    void reorderExhibitionContributions(@PathVariable Integer exhibitionId,
                                        @PathVariable Integer contributionId,
                                        @RequestBody ReorderContributionRequestDTO reorderRequest) {
        exhibitionService.reorderExhibitionContributions(exhibitionId, contributionId,
            reorderRequest.getOldContributionOrderNumber(),
            reorderRequest.getNewContributionOrderNumber());
    }

    @GetMapping("/identifier-usage/{exhibitionId}")
    @PreAuthorize("hasAuthority('EDIT_EXHIBITIONS')")
    public boolean checkIdentifierUsage(@PathVariable Integer exhibitionId,
                                        @RequestParam String identifier) {
        return exhibitionService.isIdentifierInUse(identifier, exhibitionId);
    }
}
