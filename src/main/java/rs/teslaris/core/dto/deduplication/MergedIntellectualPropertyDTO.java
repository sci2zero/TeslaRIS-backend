package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedIntellectualPropertyDTO extends MergedDocumentsDTO {

    private IntellectualPropertyDTO leftIntellectualProperty;

    private IntellectualPropertyDTO rightIntellectualProperty;
}
