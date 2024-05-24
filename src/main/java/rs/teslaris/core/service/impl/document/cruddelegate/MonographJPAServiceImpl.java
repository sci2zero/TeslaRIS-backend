package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class MonographJPAServiceImpl extends JPAServiceImpl<Monograph> {

    private final MonographRepository monographRepository;

    @Autowired
    public MonographJPAServiceImpl(MonographRepository monographRepository) {
        this.monographRepository = monographRepository;
    }

    @Override
    protected JpaRepository<Monograph, Integer> getEntityRepository() {
        return monographRepository;
    }
}
