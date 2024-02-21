package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class DatasetJPAServiceImpl extends JPAServiceImpl<Dataset> {

    private final DatasetRepository datasetRepository;

    @Autowired
    public DatasetJPAServiceImpl(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Override
    protected JpaRepository<Dataset, Integer> getEntityRepository() {
        return datasetRepository;
    }
}
