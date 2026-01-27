package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.service.impl.commontypes.NotificationServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.handlerimpl.NewOtherNameNotificationHandler;

@SpringBootTest
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NewOtherNameNotificationHandler newOtherNameNotificationHandler;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private NotificationServiceImpl notificationService;


    @Test
    void testGetUserNotifications() {
        // Given
        var userId = 1;
        var user = new User();
        user.setId(userId);
        user.setPreferredUILanguage(new LanguageTag(LanguageAbbreviations.SERBIAN, "Srpski"));

        var values = new HashMap<String, String>();
        values.put("fisrtname", "Ivan");
        values.put("middlename", "Radomir");
        values.put("lastname", "Mrsulja");

        var notification1 =
            new Notification("Notification 1", values, NotificationType.NEW_PAPER_HARVESTED, user);
        notification1.setCreateDate(new Date());
        var notification2 =
            new Notification("Notification 2", values, NotificationType.NEW_OTHER_NAME_DETECTED,
                user);
        notification2.setCreateDate(new Date());
        var notificationList = Arrays.asList(notification1, notification2);
        when(notificationRepository.getNotificationsForUser(userId)).thenReturn(notificationList);
        when(messageSource.getMessage(any(), any(), any())).thenReturn("action");

        // When
        var result = notificationService.getUserNotifications(userId);

        // Then
        assertEquals(2, result.size());
        assertEquals("Notification 1", result.get(0).getNotificationText());
        assertEquals("Notification 2 action", result.get(1).getNotificationText());
    }

    @Test
    void testApproveNewOtherNameNotification() {
        // Given
        var notificationId = 1;
        var userId = 1;
        var user = new User();
        user.setId(userId);

        var values = new HashMap<String, String>();
        values.put("fisrtname", "Ivan");
        values.put("middlename", "Radomir");
        values.put("lastname", "Mrsulja");

        var notification =
            new Notification("Notification 1", values, NotificationType.NEW_OTHER_NAME_DETECTED,
                user);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        notificationService.performAction(notificationId, userId, NotificationAction.APPROVE);

        // Then
        verify(newOtherNameNotificationHandler).handle(notification, NotificationAction.APPROVE);
    }

    @Test
    void testRejectNotification() {
        // Given
        Integer notificationId = 1;
        Integer userId = 1;
        var user = new User();
        user.setId(userId);

        var values = new HashMap<String, String>();
        values.put("fisrtname", "Ivan");
        values.put("middlename", "Radomir");
        values.put("lastname", "Mrsulja");

        var notification =
            new Notification("Notification 1", values, NotificationType.NEW_OTHER_NAME_DETECTED,
                user);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        notificationService.dismiss(notificationId, userId);

        // Then
        verify(notificationRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTestRejectNotificationFOrInvalidUser() {
        // Given
        var notificationId = 1;
        var userId = 2; // Different user
        var user = new User();
        user.setId(1);

        var notification =
            new Notification("Notification", new HashMap<>(), NotificationType.NEW_PAPER_HARVESTED,
                user);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        assertThrows(NotificationException.class, () -> {
            notificationService.dismiss(notificationId, userId);
        });

        // Then (NotificationException should be thrown)
    }

    @Test
    void shouldDeleteAllNotificationsForUser() {
        // Given
        var userId = 42;

        // When
        notificationService.dismissAll(userId);

        // Then
        verify(notificationRepository, times(1)).deleteAllForUser(userId);
    }

    @Test
    void shouldCleanPastNotificationsOfGivenType() {
        // Given
        int userId = 42;
        var type = NotificationType.NEW_DOCUMENTS_FOR_VALIDATION;

        var n1 = new Notification();
        var n2 = new Notification();

        when(notificationRepository.getNotificationsForUserAndType(userId, type))
            .thenReturn(List.of(n1, n2));

        // When
        notificationService.cleanPastNotificationsOfType(userId, type);

        // Then
        verify(notificationRepository).getNotificationsForUserAndType(userId, type);
        verify(notificationRepository, atLeastOnce()).delete(n1);
        verify(notificationRepository, atLeastOnce()).delete(n2);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void shouldDeleteOldPotentialClaimsAndSaveNew_whenNotificationIsOfTypePotentialClaims() {
        // Given
        var user = new User();
        user.setId(101);

        var notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.FOUND_POTENTIAL_CLAIMS);

        when(notificationRepository.save(notification)).thenReturn(notification);

        // When
        var result = notificationService.createNotification(notification);

        // Then
        verify(notificationRepository).deleteNewPotentialClaimsNotificationsForUser(user.getId());
        verify(notificationRepository).save(notification);
        assertEquals(notification, result);
    }

    @Test
    void shouldOnlySaveNotification_whenNotificationTypeIsNotPotentialClaims() {
        // Given
        var user = new User();
        user.setId(202);

        var notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.NEW_DOCUMENTS_FOR_VALIDATION);

        when(notificationRepository.save(notification)).thenReturn(notification);

        // When
        var result = notificationService.createNotification(notification);

        // Then
        verify(notificationRepository, never())
            .deleteNewPotentialClaimsNotificationsForUser(anyInt());
        verify(notificationRepository).save(notification);
        assertEquals(notification, result);
    }
}
