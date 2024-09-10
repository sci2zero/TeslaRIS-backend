package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.ThesisDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedThesesDTO extends MergedDocumentsDTO {

    private ThesisDTO leftThesis;

    private ThesisDTO rightThesis;
}
