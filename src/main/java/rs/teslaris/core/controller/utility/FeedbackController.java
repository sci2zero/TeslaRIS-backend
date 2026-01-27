package rs.teslaris.core.controller.utility;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.commontypes.ContactFormContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.FeedbackService;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;
import rs.teslaris.core.util.exceptionhandling.exception.CaptchaException;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    private final ReCaptchaService reCaptchaService;


    @PostMapping
    @Idempotent
    public void sendFeedback(@RequestBody @Valid ContactFormContentDTO contactFormContent) {
        if (!reCaptchaService.isCaptchaValid(contactFormContent.captchaToken())) {
            throw new CaptchaException("Invalid captcha solution.");
        }

        feedbackService.sendFeedbackMessage(contactFormContent);
    }
}
