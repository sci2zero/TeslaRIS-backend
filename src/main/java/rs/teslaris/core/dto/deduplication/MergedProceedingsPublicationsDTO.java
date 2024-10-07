package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedProceedingsPublicationsDTO extends MergedDocumentsDTO {

    private ProceedingsPublicationDTO leftProceedingsPublication;

    private ProceedingsPublicationDTO rightProceedingsPublication;
}
