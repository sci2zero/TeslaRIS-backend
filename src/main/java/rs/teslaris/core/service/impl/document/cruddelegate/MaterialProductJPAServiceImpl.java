package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.repository.document.MaterialProductRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class MaterialProductJPAServiceImpl extends JPAServiceImpl<MaterialProduct> {

    private final MaterialProductRepository materialProductRepository;

    @Autowired
    public MaterialProductJPAServiceImpl(MaterialProductRepository materialProductRepository) {
        this.materialProductRepository = materialProductRepository;
    }

    @Override
    protected JpaRepository<MaterialProduct, Integer> getEntityRepository() {
        return materialProductRepository;
    }
}
