package rs.teslaris.core.util.email;

import jakarta.mail.MessagingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    private static final Map<Class<? extends Exception>, LocalDate> exceptionThrottleMap =
        new ConcurrentHashMap<>();

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

    @Value("${mail.system-error.address}")
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

    @Async("taskExecutor")
    @Retryable(
        retryFor = {MailException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 5000)
    )
    public CompletableFuture<Boolean> sendHTMLSupportedEmail(String to, String subject,
                                                             String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (MessagingException | MailException e) {
            log.error("HTML email to user {} cannot be sent, reason: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
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
                                            Exception ex,
                                            boolean mustSend) {

        var exceptionClass = ex.getClass();
        var today = LocalDate.now();

        if (!mustSend) {
            var lastSent = exceptionThrottleMap.get(exceptionClass);
            if (Objects.nonNull(lastSent) && lastSent.isEqual(today)) {
                log.warn("Skipping email for {} — already sent today",
                    exceptionClass.getSimpleName());
                return;
            }
        }

        exceptionThrottleMap.put(exceptionClass, today);

        var message = new SimpleMailMessage();
        message.setFrom(emailAddress);
        message.setTo(systemAdminAddress);

        var importError = exceptionId.equals("IMPORT ERROR");
        if (importError) {
            message.setSubject("TeslaRIS - Import Error Occurred");
        } else {
            message.setSubject("TeslaRIS - Unhandled Exception Occurred (" + exceptionId + ")");
        }

        var stackTraceWriter = new StringWriter();
        var stackTracePrinter = new PrintWriter(stackTraceWriter);
        ex.printStackTrace(stackTracePrinter);

        message.setText(MessageFormat.format(
            (importError ? "Import" : "Unhandled") +
                " error (ID:{0}) occurred at: {2}.\nMessage: {1}\nRequest path: {3}\nTracing context id: {5}\n\nFull stack trace:\n{4}",
            exceptionId, ex.getMessage(), LocalDateTime.now(), requestPath,
            stackTraceWriter.toString(), tracingContextId));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("(CRITICAL) Unhandled error email cannot be sent, reason: {}",
                e.getMessage());
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
