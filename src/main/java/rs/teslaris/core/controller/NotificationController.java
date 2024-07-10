package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.NotificationDTO;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.notificationhandling.NotificationAction;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private final JwtUtil tokenUtil;

    @GetMapping
    public List<NotificationDTO> getAllNotificationsForUser(
        @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        return notificationService.getUserNotifications(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/count")
    public long getNotificationCountForUser(
        @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        return notificationService.getUserNotificationCount(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/{notificationId}/perform")
    public void performNotificationAction(@PathVariable Integer notificationId,
                                          @RequestHeader(value = "Authorization", required = false)
                                          String bearerToken,
                                          @RequestParam NotificationAction action) {
        notificationService.performAction(notificationId,
            tokenUtil.extractUserIdFromToken(bearerToken), action);
    }

    @DeleteMapping("/{notificationId}/dismiss")
    public void dismissNotification(@PathVariable Integer notificationId,
                                    @RequestHeader(value = "Authorization", required = false)
                                    String bearerToken) {
        notificationService.dismiss(notificationId,
            tokenUtil.extractUserIdFromToken(bearerToken));
    }
}
