package rs.teslaris.core.util.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailUtil {

    private final JavaMailSender mailSender;

    @Value("${mail.sender.address}")
    private String emailAddress;

    @Value("${mail.universal-editor.address}")
    private String universalEditorAddress;

    @Value("${mail.system-admin.address}")
    private String systemAdminAddress;


    @Async("taskExecutor")
    @Retryable(
        retryFor = {MailException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 5000)
    )
    public CompletableFuture<Boolean> sendSimpleEmail(String to, String subject, String text) {
        var message = new SimpleMailMessage();
        message.setFrom(emailAddress);
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
    public void notifyInstitutionalEditor(Integer entityId, String entityType) {
        // TODO: Ovo treba da se salje INSTITUTIONAL_EDITOR-u a da fallback bude ovo!
        sendSimpleEmail(universalEditorAddress, "New " + entityType + " added",
            "New event is added. Check it out on <BASE_URL>/api/" + entityType + "/" + entityId);
    }
}
