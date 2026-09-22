package rs.teslaris.revisioner.indexrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;

@Repository
public interface DataQualityAssessmentIndexRepository extends
    ElasticsearchRepository<DataQualityAssessmentIndex, String> {

    Optional<DataQualityAssessmentIndex> findByDatabaseId(Integer databaseId);

    List<DataQualityAssessmentIndex> findByEntityTypeAndEntityIdAndIsLatestTrue(
        String entityType, Integer entityId);

    Optional<DataQualityAssessmentIndex> findByEntityTypeAndEntityIdAndProfileNameAndIsLatestTrue(
        String entityType, Integer entityId, String profileName);

    Page<DataQualityAssessmentIndex> findByTargetAndProfileNameAndIsLatestTrueAndRelatedPersonIds(
        String target, String profileName, Integer personId, Pageable pageable);

    Page<DataQualityAssessmentIndex> findByTargetAndProfileNameAndIsLatestTrueAndOrganisationUnitIds(
        String target, String profileName, Integer organisationUnitId, Pageable pageable);
}
