package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.ProceedingsDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedProceedingsDTO extends MergedDocumentsDTO {

    private ProceedingsDTO leftProceedings;

    private ProceedingsDTO rightProceedings;
}
