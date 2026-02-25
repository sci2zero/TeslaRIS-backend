package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.model.document.MaterialProduct;

@Service
public interface MaterialProductService {

    MaterialProduct findMaterialProductById(Integer materialProductId);

    MaterialProductDTO readMaterialProductById(Integer materialProductId);

    MaterialProduct createMaterialProduct(MaterialProductDTO materialProductDTO, Boolean index);

    void editMaterialProduct(Integer materialProductId, MaterialProductDTO materialProductDTO);

    void deleteMaterialProduct(Integer materialProductId);

    void reindexMaterialProducts();

    void indexMaterialProduct(MaterialProduct materialProduct);
}
