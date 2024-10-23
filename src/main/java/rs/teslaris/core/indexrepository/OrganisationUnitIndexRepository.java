package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;

@Repository
public interface OrganisationUnitIndexRepository
    extends ElasticsearchRepository<OrganisationUnitIndex, String> {

    Optional<OrganisationUnitIndex> findOrganisationUnitIndexByDatabaseId(Integer databaseId);

    Page<OrganisationUnitIndex> findOrganisationUnitIndexesBySuperOUId(Integer superOUId,
                                                                       Pageable pageable);

    Optional<OrganisationUnitIndex> findOrganisationUnitIndexByScopusAfid(String scopusAfid);

    long count();
}
