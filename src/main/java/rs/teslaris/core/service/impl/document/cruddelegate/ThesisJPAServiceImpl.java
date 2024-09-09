package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class ThesisJPAServiceImpl extends JPAServiceImpl<Thesis> {

    private final ThesisRepository thesisRepository;

    @Autowired
    public ThesisJPAServiceImpl(ThesisRepository thesisRepository) {
        this.thesisRepository = thesisRepository;
    }

    @Override
    protected JpaRepository<Thesis, Integer> getEntityRepository() {
        return thesisRepository;
    }
}
