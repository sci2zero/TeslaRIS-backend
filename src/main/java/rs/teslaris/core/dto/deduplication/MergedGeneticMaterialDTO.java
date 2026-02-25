package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedGeneticMaterialDTO extends MergedDocumentsDTO {

    private GeneticMaterialDTO leftGeneticMaterial;

    private GeneticMaterialDTO rightGeneticMaterial;
}
