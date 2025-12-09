package rs.teslaris.core.unit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.ContactFormContentDTO;
import rs.teslaris.core.service.impl.commontypes.FeedbackServiceImpl;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.email.EmailUtil;

@SpringBootTest
public class FeedbackServiceTest {

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;


    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackServiceImpl(emailUtil, userService);

        Field feedbackEmailField;
        try {
            feedbackEmailField = FeedbackServiceImpl.class.getDeclaredField("feedbackEmail");
            feedbackEmailField.setAccessible(true);
            String testFeedbackEmail = "feedback@example.com";
            feedbackEmailField.set(feedbackService, testFeedbackEmail);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldSendFeedbackMessage() {
        // Given
        var dto = new ContactFormContentDTO(
            "John Doe", "user@example.com", "Feedback subject", "This is a message", "token"
        );

        // When
        feedbackService.sendFeedbackMessage(dto);

        // Then
        verify(emailUtil).sendFeedbackEmail(
            eq("user@example.com"),
            eq("feedback@example.com"),
            eq("Feedback subject - John Doe"),
            startsWith("This is a message\n\nJohn Doe\n")
        );
    }
}
