package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.OtherEvent;
import rs.teslaris.core.repository.document.OtherEventRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class OtherEventJPAServiceImpl extends JPAServiceImpl<OtherEvent> {

    private final OtherEventRepository otherEventRepository;


    @Autowired
    public OtherEventJPAServiceImpl(OtherEventRepository otherEventRepository) {
        this.otherEventRepository = otherEventRepository;
    }

    @Override
    protected JpaRepository<OtherEvent, Integer> getEntityRepository() {
        return otherEventRepository;
    }
}
