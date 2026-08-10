package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class MaterialProductRevisionRestorer implements RevisionRestorer<MaterialProductDTO> {

    private final MaterialProductService materialProductService;


    @Override
    public String entityType() {
        return DocumentPublicationType.MATERIAL_PRODUCT.name();
    }

    @Override
    public Class<MaterialProductDTO> dtoClass() {
        return MaterialProductDTO.class;
    }

    @Override
    public void restore(Integer entityId, MaterialProductDTO dto) {
        materialProductService.editMaterialProduct(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return materialProductService.readMaterialProductById(entityId);
    }
}
