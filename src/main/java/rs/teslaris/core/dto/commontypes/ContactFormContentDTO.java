package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactFormContentDTO(
    @NotBlank(message = "You have to provide a name.")
    String name,

    @NotBlank(message = "You have to provide your email address.")
    @Email(message = "You have to provide a valid email address.")
    String senderEmail,

    @NotBlank(message = "You have to provide a subject.")
    String subject,

    @NotBlank(message = "You have to provide a message.")
    String message,

    @NotBlank(message = "You have to provide a reCaptcha token.")
    String captchaToken
) {
}
