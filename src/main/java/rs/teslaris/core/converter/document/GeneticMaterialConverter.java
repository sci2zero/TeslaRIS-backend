package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.model.document.GeneticMaterial;

public class GeneticMaterialConverter extends DocumentPublicationConverter {

    public static GeneticMaterialDTO toDTO(GeneticMaterial geneticMaterial) {
        var geneticMaterialDTO = new GeneticMaterialDTO();

        setCommonFields(geneticMaterial, geneticMaterialDTO);

        geneticMaterialDTO.setInternalNumber(geneticMaterial.getInternalNumber());
        geneticMaterialDTO.setGeneticMaterialType(geneticMaterial.getGeneticMaterialType());

        if (Objects.nonNull(geneticMaterial.getPublisher())) {
            geneticMaterialDTO.setPublisherId(geneticMaterial.getPublisher().getId());
        } else {
            geneticMaterialDTO.setAuthorReprint(geneticMaterial.getAuthorReprint());
        }

        return geneticMaterialDTO;
    }
}
