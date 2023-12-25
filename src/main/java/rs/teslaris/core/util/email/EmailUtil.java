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

    @Value("${mail.universal-editor.address}")
    private String universalEditorAddress;

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
        // TODO: Ovo treba da se salje INSTITUTIONAL_EDITOR-u a da fallback bude ovo!
        sendSimpleEmail(universalEditorAddress, "New " + entityType + " added",
            "New event is added. Check it out on <BASE_URL>/api/" + entityType + "/" + entityId);
    }
}
