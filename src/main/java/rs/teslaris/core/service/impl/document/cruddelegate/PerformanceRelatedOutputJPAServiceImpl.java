package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;
import rs.teslaris.core.repository.document.PerformanceRelatedOutputRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PerformanceRelatedOutputJPAServiceImpl
    extends JPAServiceImpl<PerformanceRelatedOutput> {

    private final PerformanceRelatedOutputRepository performanceRelatedOutputRepository;


    @Autowired
    public PerformanceRelatedOutputJPAServiceImpl(
        PerformanceRelatedOutputRepository performanceRelatedOutputRepository) {
        this.performanceRelatedOutputRepository = performanceRelatedOutputRepository;
    }

    @Override
    protected JpaRepository<PerformanceRelatedOutput, Integer> getEntityRepository() {
        return performanceRelatedOutputRepository;
    }
}
