package rs.teslaris.revisioner.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "rs.teslaris.revisioner.indexrepository")
public class RevisionerElasticsearchConfiguration {
}
