package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.MaterialProductDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedMaterialProductDTO extends MergedDocumentsDTO {

    private MaterialProductDTO leftMaterialProduct;

    private MaterialProductDTO rightMaterialProduct;
}
