package rs.teslaris.core.util.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender mailSender;

    @Value("${mail.sender.address}")
    private String emailAddress;

    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        var message = new SimpleMailMessage();
        message.setFrom(emailAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            assert true; // TODO: Maybe log network error, that the email could ot be sent?
        }
    }

    @Async
    public void notifyInstitutionalEditor(Integer entityId, String entityType) {
        // TODO: Ovo treba da se salje INSTITUTIONAL_EDITOR-u ili ADMIN-u i da bude konfiguraciono iz application.properties!
        sendSimpleEmail("change@change.com", "New " + entityType + " added",
            "New event is added. Check it out on <BASE_URL>/" + entityId);
    }
}
