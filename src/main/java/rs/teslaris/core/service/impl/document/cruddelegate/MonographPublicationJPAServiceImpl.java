package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class MonographPublicationJPAServiceImpl extends JPAServiceImpl<MonographPublication> {

    private final MonographPublicationRepository monographPublicationRepository;

    @Autowired
    public MonographPublicationJPAServiceImpl(
        MonographPublicationRepository monographPublicationRepository) {
        this.monographPublicationRepository = monographPublicationRepository;
    }

    @Override
    protected JpaRepository<MonographPublication, Integer> getEntityRepository() {
        return monographPublicationRepository;
    }
}
