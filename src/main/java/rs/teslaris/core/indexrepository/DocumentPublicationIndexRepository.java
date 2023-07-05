package rs.teslaris.core.indexrepository;


import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Repository
public interface DocumentPublicationIndexRepository extends
    ElasticsearchRepository<DocumentPublicationIndex, String> {
}
