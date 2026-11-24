package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.person.LanguageLevel;

/**
 * A language a person knows. The name comes from the language, so the client sends no name.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageKnowledgeRequestDTO {

    @NotNull(message = "You have to provide a language.")
    private Integer languageId;

    private Boolean motherTongue;

    private LanguageLevel reading;

    private LanguageLevel writing;

    private LanguageLevel speaking;

    private LanguageLevel listening;

    private LanguageLevel academicReview;

    private LanguageLevel academicWriting;

    private LanguageLevel overall;
}
