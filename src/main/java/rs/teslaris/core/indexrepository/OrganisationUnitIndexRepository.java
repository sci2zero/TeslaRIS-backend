package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;

@Repository
public interface OrganisationUnitIndexRepository
    extends ElasticsearchRepository<OrganisationUnitIndex, String> {

    Optional<OrganisationUnitIndex> findOrganisationUnitIndexByDatabaseId(Integer databaseId);
}
