package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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

    private String languageTag;

    @NotBlank(message = "You have to provide content.")
    private String content;

    @Positive(message = "Priority must be a positive number.")
    private int priority;


    public MultilingualContentDTO(MultilingualContentDTO other) {
        this.languageTagId = other.languageTagId;
        this.languageTag = other.languageTag;
        this.content = other.content;
        this.priority = other.priority;
    }
}
