package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.repository.document.SoftwareRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class SoftwareJPAServiceImpl extends JPAServiceImpl<Software> {

    private final SoftwareRepository softwareRepository;

    @Autowired
    public SoftwareJPAServiceImpl(SoftwareRepository softwareRepository) {
        this.softwareRepository = softwareRepository;
    }

    @Override
    protected JpaRepository<Software, Integer> getEntityRepository() {
        return softwareRepository;
    }
}
