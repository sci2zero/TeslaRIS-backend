package rs.teslaris.core.indexrepository.statistics;

import java.time.LocalDateTime;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.statistics.StatisticsIndex;

@Repository
public interface StatisticsIndexRepository
    extends ElasticsearchRepository<StatisticsIndex, String> {

    Integer countByTimestampBetweenAndTypeAndDocumentId(LocalDateTime from, LocalDateTime to,
                                                        String type, Integer documentId);

    Integer countByTimestampBetweenAndTypeAndPersonId(LocalDateTime from, LocalDateTime to,
                                                        String type, Integer personId);

    Integer countByTimestampBetweenAndTypeAndOrganisationUnitId(LocalDateTime from, LocalDateTime to,
                                                      String type, Integer organisationUnitId);

}
