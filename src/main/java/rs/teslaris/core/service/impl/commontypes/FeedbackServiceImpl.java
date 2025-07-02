package rs.teslaris.core.service.impl.commontypes;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ContactFormContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.FeedbackService;
import rs.teslaris.core.util.email.EmailUtil;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final EmailUtil emailUtil;

    @Value("${feedback.email}")
    private String feedbackEmail;


    @Override
    public void sendFeedbackMessage(ContactFormContentDTO contactFormContent) {
        var subject = contactFormContent.subject() + " - " + contactFormContent.name();

        var currentTime = LocalDateTime.now().toString();
        var message = contactFormContent.message() + "\n\n" + contactFormContent.name() + "\n" +
            currentTime.split("T")[0] + " " + currentTime.split("T")[1].split("\\.")[0];

        emailUtil.sendFeedbackEmail(contactFormContent.senderEmail(), feedbackEmail, subject,
            message);
    }
}
