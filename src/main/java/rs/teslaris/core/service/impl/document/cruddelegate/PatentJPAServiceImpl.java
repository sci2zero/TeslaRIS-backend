package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PatentJPAServiceImpl extends JPAServiceImpl<Patent> {

    private final PatentRepository patentRepository;

    @Autowired
    public PatentJPAServiceImpl(PatentRepository patentRepository) {
        this.patentRepository = patentRepository;
    }

    @Override
    protected JpaRepository<Patent, Integer> getEntityRepository() {
        return patentRepository;
    }
}
