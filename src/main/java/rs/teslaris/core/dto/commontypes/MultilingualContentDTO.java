package rs.teslaris.core.dto.commontypes;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MultilingualContentDTO {

    @Positive(message = "Language tag ID must be a positive number.")
    private Integer languageTagId;

    @NotBlank(message = "You have to provide content.")
    private String content;

    @Positive(message = "Priority must be a positive number.")
    private int priority;
}
