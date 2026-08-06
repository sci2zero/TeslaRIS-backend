package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.GeneticMaterialService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class GeneticMaterialRevisionRestorer implements RevisionRestorer<GeneticMaterialDTO> {

    private final GeneticMaterialService geneticMaterialService;


    @Override
    public String entityType() {
        return DocumentPublicationType.GENETIC_MATERIAL.name();
    }

    @Override
    public Class<GeneticMaterialDTO> dtoClass() {
        return GeneticMaterialDTO.class;
    }

    @Override
    public void restore(Integer entityId, GeneticMaterialDTO dto) {
        geneticMaterialService.editGeneticMaterial(entityId, dto);
    }
}
