package rs.teslaris.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisResponseDTO extends ThesisDTO {

    private String languageCode;

    private Boolean isOnPublicReview;
}
