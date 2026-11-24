package rs.teslaris.core.dto.person;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.person.LanguageLevel;

/**
 * A language a person knows. It is not an expertise or a skill here, so it carries no research
 * area, although it shares a table with them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageKnowledgeResponseDTO {

    private Integer id;

    private List<MultilingualContentDTO> name;

    private LanguageResponseDTO language;

    private Boolean motherTongue;

    private LanguageLevel reading;

    private LanguageLevel writing;

    private LanguageLevel speaking;

    private LanguageLevel listening;

    private LanguageLevel academicReview;

    private LanguageLevel academicWriting;

    private LanguageLevel overall;

    private List<DocumentFileResponseDTO> proofs;


    public LanguageKnowledgeResponseDTO(LanguageKnowledgeResponseDTO other) {
        this.id = other.id;
        this.name = Objects.nonNull(other.name)
            ? other.name.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.language = Objects.nonNull(other.language)
            ? new LanguageResponseDTO(other.language.getId(), other.language.getLanguageCode(),
            Objects.nonNull(other.language.getName())
                ? other.language.getName().stream().map(MultilingualContentDTO::new)
                .collect(Collectors.toList())
                : null)
            : null;
        this.motherTongue = other.motherTongue;
        this.reading = other.reading;
        this.writing = other.writing;
        this.speaking = other.speaking;
        this.listening = other.listening;
        this.academicReview = other.academicReview;
        this.academicWriting = other.academicWriting;
        this.overall = other.overall;
        this.proofs = Objects.nonNull(other.proofs)
            ? other.proofs.stream().map(DocumentFileResponseDTO::new).collect(Collectors.toList())
            : null;
    }
}
