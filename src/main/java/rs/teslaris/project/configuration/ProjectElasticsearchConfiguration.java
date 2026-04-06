package rs.teslaris.project.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "rs.teslaris.project.indexrepository")
public class ProjectElasticsearchConfiguration {
}
