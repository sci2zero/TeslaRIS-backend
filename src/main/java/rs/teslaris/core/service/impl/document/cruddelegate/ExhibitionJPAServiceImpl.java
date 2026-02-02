package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Exhibition;
import rs.teslaris.core.repository.document.ExhibitionRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class ExhibitionJPAServiceImpl extends JPAServiceImpl<Exhibition> {

    private final ExhibitionRepository exhibitionRepository;


    @Autowired
    public ExhibitionJPAServiceImpl(ExhibitionRepository exhibitionRepository) {
        this.exhibitionRepository = exhibitionRepository;
    }

    @Override
    protected JpaRepository<Exhibition, Integer> getEntityRepository() {
        return exhibitionRepository;
    }
}
