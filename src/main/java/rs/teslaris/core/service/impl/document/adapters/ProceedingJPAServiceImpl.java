package rs.teslaris.core.service.impl.document.adapters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class ProceedingJPAServiceImpl extends JPAServiceImpl<Proceedings> {

    private final ProceedingsRepository proceedingsRepository;

    @Autowired
    public ProceedingJPAServiceImpl(ProceedingsRepository proceedingsRepository) {
        this.proceedingsRepository = proceedingsRepository;
    }

    @Override
    protected JpaRepository<Proceedings, Integer> getEntityRepository() {
        return proceedingsRepository;
    }
}
