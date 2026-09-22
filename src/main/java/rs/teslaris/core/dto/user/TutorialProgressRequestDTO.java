package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.user.TutorialStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorialProgressRequestDTO {

    @NotBlank(message = "Tutorial key cannot be blank.")
    private String tutorialKey;

    @NotNull(message = "Tutorial status cannot be null.")
    private TutorialStatus status;

    @PositiveOrZero(message = "Step must be a non-negative number.")
    private Integer step;
}
