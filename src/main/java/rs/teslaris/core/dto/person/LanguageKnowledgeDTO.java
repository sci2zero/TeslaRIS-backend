package rs.teslaris.core.dto.person;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.person.LanguageLevel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LanguageKnowledgeDTO extends ExpertiseOrSkillDTO {

    private Integer languageId;

    private Boolean motherTongue;

    private LanguageLevel read;

    private LanguageLevel write;

    private LanguageLevel speak;

    private LanguageLevel understandSpoken;

    private LanguageLevel peerReview;
}
