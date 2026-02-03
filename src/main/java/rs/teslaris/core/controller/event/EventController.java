package rs.teslaris.core.controller.event;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
@Traceable
public class EventController {

    private final JwtUtil tokenUtil;

    private final UserService userService;

    private final EventService eventService;


    @GetMapping("/simple-search")
    Page<EventIndex> searchConferences(
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
        @RequestParam(value = "eventTypes", required = false) List<EventType> eventTypes,
        @RequestHeader(value = "Authorization", defaultValue = "") String bearerToken,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);

        if (!bearerToken.isEmpty()) {
            if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.name())) {
                return eventService.searchEvents(
                    tokens, pageable, eventTypes,
                    returnOnlyNonSerialEvents, returnOnlySerialEvents,
                    null, unclassified ? commissionId : null,
                    emptyEventsOnly);
            } else if (tokenUtil.extractUserRoleFromToken(bearerToken)
                .equals(UserRole.COMMISSION.name())) {
                var userId = tokenUtil.extractUserIdFromToken(bearerToken);

                return eventService.searchEvents(
                    tokens, pageable, eventTypes,
                    returnOnlyNonSerialEvents, returnOnlySerialEvents,
                    forMyInstitution ? userService.getUserOrganisationUnitId(userId) : null,
                    unclassified ? userService.getUserCommissionId(userId) : null,
                    false);
            }
        }

        return eventService.searchEvents(
            tokens, pageable, eventTypes,
            returnOnlyNonSerialEvents, returnOnlySerialEvents,
            null, null, false
        );
    }
}
