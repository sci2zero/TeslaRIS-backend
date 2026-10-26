package rs.teslaris.revisioner.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.revisioner.model.fixator.DataQualityFixatorPipeline;

@Repository
public interface DataQualityFixatorPipelineRepository
    extends JpaRepository<DataQualityFixatorPipeline, Integer> {

    Optional<DataQualityFixatorPipeline> findByPipelineName(String pipelineName);

    List<DataQualityFixatorPipeline> findAllByOrderByPipelineNameAsc();
}
