package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.model.document.GeneticMaterial;

@Service
public interface GeneticMaterialService {

    GeneticMaterial findGeneticMaterialById(Integer geneticMaterialId);

    GeneticMaterialDTO readGeneticMaterialById(Integer geneticMaterialId);

    GeneticMaterial createGeneticMaterial(GeneticMaterialDTO geneticMaterialDTO, Boolean index);

    void editGeneticMaterial(Integer geneticMaterialId, GeneticMaterialDTO geneticMaterialDTO);

    void deleteGeneticMaterial(Integer geneticMaterialId);

    void reindexGeneticMaterials();

    void indexGeneticMaterial(GeneticMaterial geneticMaterial);
}
