package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.Notification;
import rs.teslaris.core.model.commontypes.NotificationSentiment;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.TaskManagerServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.notificationhandling.NotificationDurationFormatter;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskNotificationTest {
    @Mock private ThreadPoolTaskScheduler taskScheduler;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private ScheduledTaskMetadataRepository metadataRepository;
    @InjectMocks private TaskManagerServiceImpl taskManager;

    @BeforeEach
    void setUpMessages() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("internationalization/messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        new NotificationFactory(messages);
    }

    @ParameterizedTest
    @CsvSource({"0,0.00s", "435155542,0.43s", "1200000000,1s", "1999999999,1s",
        "59600000000,59s", "65000000000,1m 5s", "3661000000000,1h 1m 1s",
        "90061000000000,25h 1m 1s"})
    void shouldFormatDurations(long nanos, String expected) {
        assertEquals(expected, NotificationDurationFormatter.format(Duration.ofNanos(nanos)));
    }

    @Test
    void shouldKeepReindexScopeOutOfTitle() {
        var values = new HashMap<>(Map.of("taskId", "DatabaseReindex-PUBLICATION-PERSON-uuid",
            "indexesToRepopulate", "[PUBLICATION, PERSON]", "duration", "1h 2m 3s"));
        var notification = NotificationFactory.contructScheduledTaskCompletedNotification(
            values, user("EN"), true);
        assertEquals("Database reindex completed successfully.", notification.getNotificationText());
        assertFalse(notification.getNotificationText().contains("uuid"));
        assertTrue(notification.getDetails().contains("Indexes: [PUBLICATION, PERSON]"));
        assertTrue(notification.getDetails().contains("Duration: 1h 2m 3s"));
        assertEquals(NotificationSentiment.SUCCESS, notification.getSentiment());
    }

    @Test
    void shouldLocaliseBackupAndPreserveFailureReasonInDetails() {
        var notification = NotificationFactory.contructScheduledBackupGenerationCompletedNotification(
            new HashMap<>(Map.of("taskId", "Document_Backup-1-2020_2026-uuid",
                "duration", "0.44s", "error", "Storage unavailable")), user("SR"), false);
        assertEquals("Bekap dokumenata: neuspešno.", notification.getNotificationText());
        assertEquals(NotificationSentiment.ERROR, notification.getSentiment());
        assertTrue(notification.getDetails().contains("Trajanje: 0.44s"));
        assertTrue(notification.getDetails().contains("Razlog neuspeha: Storage unavailable"));
    }

    @Test
    void shouldPreserveLongTaskIdsAndErrorsInDetailsWithoutActionValueLengthLimits() {
        var taskId = "DatabaseReindex-" + "PUBLICATION-".repeat(30) + "uuid";
        var error = "Storage failure: " + "Error details ".repeat(30);
        var notification = NotificationFactory.contructScheduledTaskCompletedNotification(
            Map.of("taskId", taskId, "error", error), user("EN"), false);
        assertEquals("Database reindex failed.", notification.getNotificationText());
        assertTrue(notification.getDetails().contains(taskId));
        assertTrue(notification.getDetails().contains(error));
        assertTrue(notification.getValues().isEmpty());
    }

    @Test
    void shouldNotifyStartAndFailureOfScheduledReindex() {
        var taskId = "DatabaseReindex-PERSON-test";
        var timestamp = Instant.now().plus(1, ChronoUnit.MINUTES);
        when(metadataRepository.findTaskByTaskId(taskId)).thenReturn(Optional.of(
            new ScheduledTaskMetadata(taskId, timestamp, ScheduledTaskType.REINDEXING,
                Map.of("indexesToRepopulate", List.of("PERSON")), RecurrenceType.ONCE)));
        when(userService.findOne(1)).thenReturn(user("EN"));
        var runnable = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.schedule(runnable.capture(), any(Instant.class)))
            .thenReturn(mock(ScheduledFuture.class));
        var notifications = new java.util.ArrayList<Notification>();
        when(notificationService.createNotification(any())).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notifications.add(notification);
            return notification;
        });

        taskManager.scheduleTask(taskId, timestamp, () -> {
            throw new IllegalStateException("Person index unavailable");
        }, 1, RecurrenceType.ONCE);
        runnable.getValue().run();

        assertEquals(2, notifications.size());
        assertEquals(NotificationSentiment.INFO, notifications.get(0).getSentiment());
        assertEquals(NotificationSentiment.ERROR, notifications.get(1).getSentiment());
        assertTrue(notifications.get(1).getDetails().contains("Person index unavailable"));
        assertTrue(notifications.get(1).getDetails().contains("Indexes: [PERSON]"));
        assertTrue(notifications.get(1).getDetails().contains("Duration:"));
        assertFalse(notifications.get(0).getDetails().contains("Failure reason:"));
        assertFalse(taskManager.isTaskScheduled(taskId));
    }

    private User user(String language) {
        var user = new User();
        user.setId(1);
        user.setPreferredUILanguage(new LanguageTag(language, language));
        return user;
    }
}
