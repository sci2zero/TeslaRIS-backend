package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.repository.document.IntangibleProductRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class IntangibleProductJPAServiceImpl extends JPAServiceImpl<IntangibleProduct> {

    private final IntangibleProductRepository intangibleProductRepository;

    @Autowired
    public IntangibleProductJPAServiceImpl(
        IntangibleProductRepository intangibleProductRepository) {
        this.intangibleProductRepository = intangibleProductRepository;
    }

    @Override
    protected JpaRepository<IntangibleProduct, Integer> getEntityRepository() {
        return intangibleProductRepository;
    }
}
