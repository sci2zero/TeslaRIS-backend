package rs.teslaris.project.indexrepository.project;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.indexmodel.project.ProjectIndex;

import java.util.Optional;

@Repository
public interface ProjectIndexRepository extends ElasticsearchRepository<ProjectIndex, String> {

    Optional<ProjectIndex> findProjectIndexByDatabaseId(Integer databaseId);
}
