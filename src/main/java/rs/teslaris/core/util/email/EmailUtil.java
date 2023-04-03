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
        mailSender.send(message);
    }
}
