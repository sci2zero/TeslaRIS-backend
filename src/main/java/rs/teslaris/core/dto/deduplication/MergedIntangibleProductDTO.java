package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.IntangibleProductDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedIntangibleProductDTO extends MergedDocumentsDTO {

    private IntangibleProductDTO leftIntangibleProduct;

    private IntangibleProductDTO rightIntangibleProduct;
}
