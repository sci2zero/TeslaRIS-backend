package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class ProceedingPublicationJPAServiceImpl extends JPAServiceImpl<ProceedingsPublication> {

    private final ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Autowired
    public ProceedingPublicationJPAServiceImpl(
        ProceedingsPublicationRepository proceedingsPublicationRepository) {
        this.proceedingsPublicationRepository = proceedingsPublicationRepository;
    }

    @Override
    protected JpaRepository<ProceedingsPublication, Integer> getEntityRepository() {
        return proceedingsPublicationRepository;
    }
}
