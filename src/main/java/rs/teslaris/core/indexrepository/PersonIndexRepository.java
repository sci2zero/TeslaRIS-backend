package rs.teslaris.core.indexrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.PersonIndex;

@Repository
public interface PersonIndexRepository extends ElasticsearchRepository<PersonIndex, String> {

    Optional<PersonIndex> findByDatabaseId(Integer databaseId);

    Page<PersonIndex> findByEmploymentInstitutionsIdIn(Pageable pageable,
                                                       List<Integer> employmentInstitutionsIds);
}
