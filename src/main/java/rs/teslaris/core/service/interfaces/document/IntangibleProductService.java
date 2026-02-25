package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.model.document.IntangibleProduct;

@Service
public interface IntangibleProductService {

    IntangibleProduct findIntangibleProductById(Integer intangibleProductId);

    IntangibleProductDTO readIntangibleProductById(Integer intangibleProductId);

    IntangibleProduct createIntangibleProduct(IntangibleProductDTO intangibleProductDTO,
                                              Boolean index);

    void editIntangibleProduct(Integer intangibleProductId,
                               IntangibleProductDTO intangibleProductDTO);

    void deleteIntangibleProduct(Integer intangibleProductId);

    void reindexIntangibleProduct();

    void indexIntangibleProduct(IntangibleProduct intangibleProduct);
}
