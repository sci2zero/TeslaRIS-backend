package rs.teslaris.thesislibrary.dto;

import jakarta.validation.constraints.NotBlank;

public record AttendanceCancellationRequest(
    @NotBlank(message = "You have to provide an attendance identifier.")
    String attendanceIdentifier,

    @NotBlank(message = "You have to provide a reCaptcha token.")
    String captchaToken
) {
}
