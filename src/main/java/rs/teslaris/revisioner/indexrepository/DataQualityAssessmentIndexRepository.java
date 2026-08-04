package rs.teslaris.revisioner.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;

@Repository
public interface DataQualityAssessmentIndexRepository extends
    ElasticsearchRepository<DataQualityAssessmentIndex, String> {

    Optional<DataQualityAssessmentIndex> findByEntityTypeAndEntityIdAndProfileNameAndIsLatestTrue(
        String entityType, Integer entityId, String profileName);
}
