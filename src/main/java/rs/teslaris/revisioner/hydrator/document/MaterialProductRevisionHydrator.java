package rs.teslaris.revisioner.hydrator.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class MaterialProductRevisionHydrator extends RevisionHydrator<MaterialProductDTO> {

    @Autowired
    public MaterialProductRevisionHydrator(
        CountryService countryService) {
        super(countryService);
    }


    @Override
    public String entityType() {
        return DocumentPublicationType.MATERIAL_PRODUCT.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(MaterialProductDTO dto) {
        hydrateCommonFields(dto);
    }
}
