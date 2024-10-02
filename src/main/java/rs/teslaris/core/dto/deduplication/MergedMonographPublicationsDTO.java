package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.MonographPublicationDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedMonographPublicationsDTO extends MergedDocumentsDTO {

    private MonographPublicationDTO leftMonographPublication;

    private MonographPublicationDTO rightMonographPublication;
}
