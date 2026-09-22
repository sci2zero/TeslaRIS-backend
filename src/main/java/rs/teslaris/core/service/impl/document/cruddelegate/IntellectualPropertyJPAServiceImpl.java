package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.IntellectualProperty;
import rs.teslaris.core.repository.document.IntellectualPropertyRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class IntellectualPropertyJPAServiceImpl extends JPAServiceImpl<IntellectualProperty> {

    private final IntellectualPropertyRepository intellectualPropertyRepository;

    @Autowired
    public IntellectualPropertyJPAServiceImpl(
        IntellectualPropertyRepository intellectualPropertyRepository) {
        this.intellectualPropertyRepository = intellectualPropertyRepository;
    }

    @Override
    protected JpaRepository<IntellectualProperty, Integer> getEntityRepository() {
        return intellectualPropertyRepository;
    }
}
