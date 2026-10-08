package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationReadStatus;
import rs.teslaris.core.model.commontypes.NotificationSentiment;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.service.impl.commontypes.NotificationServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotificationException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.notificationhandling.NotificationAction;
import rs.teslaris.core.util.notificationhandling.handlerimpl.NewOtherNameNotificationHandler;

@ExtendWith(MockitoExtension.class)
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
        when(notificationRepository.getNotificationsForUserByReadStatus(userId, false, PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(notificationList, PageRequest.of(0, 10), 2));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("action");

        // When
        var result = notificationService.getUserNotifications(userId, NotificationReadStatus.UNREAD, PageRequest.of(0, 10));

        // Then
        assertEquals(2, result.getNumberOfElements());
        assertEquals("Notification 1", result.getContent().get(0).getNotificationText());
        assertEquals("Notification 2 action", result.getContent().get(1).getNotificationText());
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
        assertNotNull(notification.getReadAt());
        assertFalse(notification.getDeleted());
        verify(notificationRepository).save(notification);
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
        assertNotNull(notification.getReadAt());
        assertFalse(notification.getDeleted());
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
    void shouldMarkAllNotificationsAsReadForUser() {
        // Given
        var userId = 42;

        // When
        notificationService.dismissAll(userId);

        // Then
        var timestamp = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).markAllAsReadForUser(
            org.mockito.ArgumentMatchers.eq(userId), timestamp.capture());
        assertNotNull(timestamp.getValue());
    }

    @Test
    void shouldListReadNotificationsWithTimestampAndNoActions() {
        var user = new User();
        user.setId(1);
        user.setPreferredUILanguage(new LanguageTag(LanguageAbbreviations.ENGLISH, "English"));
        var notification = new Notification("Read notification", new HashMap<>(),
            NotificationType.NEW_PAPER_HARVESTED, user);
        notification.setCreateDate(new Date());
        notification.setReadAt(LocalDateTime.of(2026, 10, 8, 12, 0));
        notification.setDetails("Duration: 1m 2s");
        notification.setSentiment(NotificationSentiment.SUCCESS);
        when(notificationRepository.getNotificationsForUserByReadStatus(1, true, PageRequest.of(1, 2)))
            .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(1, 2), 3));

        var result = notificationService.getUserNotifications(1, NotificationReadStatus.READ, PageRequest.of(1, 2));

        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        verify(notificationRepository).getNotificationsForUserByReadStatus(1, true, PageRequest.of(1, 2));
        assertEquals(notification.getReadAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime(),
            result.getContent().getFirst().getReadAt());
        assertEquals("Duration: 1m 2s", result.getContent().getFirst().getDetails());
        assertEquals(NotificationSentiment.SUCCESS, result.getContent().getFirst().getSentiment());
        assertEquals(List.of(), result.getContent().getFirst().getPossibleActions());
    }

    @Test
    void shouldListAllNotificationsWithoutReadFilter() {
        when(notificationRepository.getNotificationsForUserByReadStatus(1, null, PageRequest.of(0, 10)))
            .thenReturn(Page.empty(PageRequest.of(0, 10)));

        assertEquals(List.of(),
            notificationService.getUserNotifications(1, NotificationReadStatus.ALL, PageRequest.of(0, 10)).getContent());
        verify(notificationRepository).getNotificationsForUserByReadStatus(1, null, PageRequest.of(0, 10));
    }

    @Test
    void shouldPreserveFirstReadTimestampWhenDismissedAgain() {
        var user = new User();
        user.setId(1);
        var notification = new Notification("Notification", new HashMap<>(),
            NotificationType.NEW_PAPER_HARVESTED, user);
        var readAt = LocalDateTime.of(2026, 10, 8, 12, 0);
        notification.setReadAt(readAt);
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));

        notificationService.dismiss(1, 1);

        assertEquals(readAt, notification.getReadAt());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldNotPerformActionOnReadNotification() {
        var user = new User();
        user.setId(1);
        var notification = new Notification("Notification", new HashMap<>(),
            NotificationType.NEW_OTHER_NAME_DETECTED, user);
        notification.setReadAt(LocalDateTime.now());
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));

        assertThrows(NotificationException.class, () ->
            notificationService.performAction(1, 1, NotificationAction.APPROVE));

        verify(newOtherNameNotificationHandler, never()).handle(any(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldNotMarkAnotherUsersNotificationAsRead() {
        var user = new User();
        user.setId(1);
        var notification = new Notification("Notification", new HashMap<>(),
            NotificationType.NEW_OTHER_NAME_DETECTED, user);
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));

        assertThrows(NotificationException.class, () ->
            notificationService.performAction(1, 2, NotificationAction.APPROVE));

        verify(newOtherNameNotificationHandler, never()).handle(any(), any());
        verify(notificationRepository, never()).save(any());
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
