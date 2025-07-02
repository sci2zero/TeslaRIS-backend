package rs.teslaris.core.util.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.teslaris.core.repository.commontypes.NotificationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailUtil {

    private final JavaMailSender mailSender;

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    private final MessageSource messageSource;

    @Value("${mail.sender.address}")
    private String emailAddress;

    @Value("${entity-creation.admin.address}")
    private String universalEditorAddress;

    @Value("${entity-creation.admin.locale}")
    private String universalEditorLocale;

    @Value("${mail.system-admin.address}")
    private String systemAdminAddress;

    @Value("${frontend.application.address}")
    private String baseFrontendUrl;


    @Async("taskExecutor")
    @Retryable(
        retryFor = {MailException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 5000)
    )
    public CompletableFuture<Boolean> sendSimpleEmail(String to, String subject, String text) {
        var message = new SimpleMailMessage();
        message.setFrom(emailAddress);

        return finishSendingEmail(message, to, subject, text);
    }

    @Async("taskExecutor")
    @Retryable(
        retryFor = {MailException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000 * 60 * 60) // try every one hour
    )
    public CompletableFuture<Boolean> sendFeedbackEmail(String from, String to, String subject,
                                                        String text) {
        var message = new SimpleMailMessage();
        message.setFrom(from);

        return finishSendingEmail(message, to, subject, text);
    }

    private CompletableFuture<Boolean> finishSendingEmail(SimpleMailMessage message, String to,
                                                          String subject, String text) {
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (MailException e) {
            log.error("Email to user " + to + " cannot be sent, reason: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("taskExecutor")
    @Retryable(
        retryFor = {MailException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 5000)
    )
    public void sendUnhandledExceptionEmail(String exceptionId,
                                            String tracingContextId,
                                            String requestPath,
                                            Exception ex) {
        var message = new SimpleMailMessage();
        message.setFrom(emailAddress);
        message.setTo(systemAdminAddress);
        message.setSubject("TeslaRIS - Unhandled Exception Occurred (" + exceptionId + ")");

        var stackTraceWriter = new StringWriter();
        var stackTracePrinter = new PrintWriter(stackTraceWriter);
        ex.printStackTrace(stackTracePrinter);

        message.setText(MessageFormat.format(
            "Unhandled error (ID:{0}) occurred at: {2}.\nMessage: {1}\nRequest path: {3}\nTracing context id: {5}\n\nFull stack trace:\n{4}",
            exceptionId, ex.getMessage(), LocalDateTime.now(), requestPath,
            stackTraceWriter.toString(), tracingContextId));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("(CRITICAL) Unhandled error email cannot be sent, reason: " + e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void notifyInstitutionalEditor(Integer entityId, String entityName, String entityType) {
        var relativePath = switch (entityType) {
            case "event" -> "events/conference/" + entityId;
            case "journal" -> "journals/" + entityId;
            case "publisher" -> "publishers/" + entityId;
            default -> "";
        };

        if (relativePath.isEmpty()) {
            return;
        }

        relativePath = universalEditorLocale + "/" + relativePath;
        var entityLandingPageURL = baseFrontendUrl + relativePath;

        entityType = messageSource.getMessage(entityType, new Object[] {},
            Locale.forLanguageTag(universalEditorLocale));

        var subject = messageSource.getMessage("mail.newEntityCreation.subject",
            new Object[] {entityType}, Locale.forLanguageTag(universalEditorLocale));
        var message = messageSource.getMessage("notification.newEntityCreation",
            new Object[] {entityType, entityName}, Locale.forLanguageTag(universalEditorLocale));

        sendSimpleEmail(universalEditorAddress, subject, message + "\n\n" + entityLandingPageURL);

        var adminUserIds = userRepository.findAllSystemAdmins();
        if (adminUserIds.isEmpty()) {
            return;
        }

        var finalRelativePath = relativePath;
        var finalEntityType = entityType;
        adminUserIds.forEach(adminUser -> {
            var notificationValues = new HashMap<String, String>();

            notificationValues.put("entityId", String.valueOf(entityId));
            notificationValues.put("entityUrl", finalRelativePath);
            notificationValues.put("entityType", finalEntityType);
            notificationValues.put("entityName", entityName);
            notificationRepository.save(
                NotificationFactory.contructNewEntityCreationNotification(notificationValues,
                    adminUser));
        });
    }
}
