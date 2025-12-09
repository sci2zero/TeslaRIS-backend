package rs.teslaris.core.service.impl.commontypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ContactFormContentDTO;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.service.interfaces.commontypes.FeedbackService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.email.EmailUtil;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final EmailUtil emailUtil;

    private final UserService userService;

    @Value("${feedback.email}")
    private String feedbackEmail;


    @Override
    public void sendFeedbackMessage(ContactFormContentDTO contactFormContent) {
        var subject = contactFormContent.subject() + " - " + contactFormContent.name();

        var currentTime = LocalDateTime.now().toString();
        var message = contactFormContent.message() + "\n\n" + contactFormContent.name() + "\n" +
            currentTime.split("T")[0] + " " + currentTime.split("T")[1].split("\\.")[0];

        var recipientEmails = new HashSet<String>();
        recipientEmails.add(feedbackEmail);
        recipientEmails.addAll(
            userService.findAllSystemAdminUsers().stream().map(User::getEmail).toList());

        recipientEmails.forEach(
            recipientEmail -> emailUtil.sendFeedbackEmail(contactFormContent.senderEmail(),
                recipientEmail, subject, message));
    }
}
