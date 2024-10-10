package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.MonographDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedMonographsDTO extends MergedDocumentsDTO {

    private MonographDTO leftMonograph;

    private MonographDTO rightMonograph;
}