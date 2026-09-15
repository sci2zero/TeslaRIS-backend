package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.IntangibleProductService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class IntangibleProductRevisionRestorer
    implements RevisionRestorer<IntangibleProductDTO> {

    private final IntangibleProductService intangibleProductService;


    @Override
    public String entityType() {
        return DocumentPublicationType.INTANGIBLE_PRODUCT.name();
    }

    @Override
    public Class<IntangibleProductDTO> dtoClass() {
        return IntangibleProductDTO.class;
    }

    @Override
    public void restore(Integer entityId, IntangibleProductDTO dto) {
        intangibleProductService.editIntangibleProduct(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return intangibleProductService.readIntangibleProductById(entityId);
    }
}
