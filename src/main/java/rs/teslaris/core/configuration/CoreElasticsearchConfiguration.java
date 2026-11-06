package rs.teslaris.core.configuration;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "rs.teslaris.core.indexrepository")
public class CoreElasticsearchConfiguration
    extends org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration {

    private final int CONNECT_TIMEOUT_SECONDS = 10;

    private final int SOCKET_TIMEOUT_SECONDS = 120;

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.username}")
    private String userName;

    @Value("${elasticsearch.password}")
    private String password;


    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder().connectedTo(host + ":" + port)
            .withBasicAuth(userName, password)
            .withConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .withSocketTimeout(Duration.ofSeconds(SOCKET_TIMEOUT_SECONDS))
            .build();
    }

    /**
     * This method builds a new client with {@link ElasticsearchClients}, because Spring returns the
     * same dead client from the {@code elasticsearchRestClient} bean method.
     */
    @Bean
    @NotNull
    @Override
    @SuppressWarnings("resource")
    public ElasticsearchTransport elasticsearchTransport(@NotNull RestClient restClient,
                                                         @NotNull JsonpMapper jsonpMapper) {
        return new ResilientElasticsearchTransport(
            restClient,
            () -> ElasticsearchClients.getRestClient(clientConfiguration()),
            rebuiltRestClient -> ElasticsearchClients.getElasticsearchTransport(
                rebuiltRestClient, ElasticsearchClients.IMPERATIVE_CLIENT, transportOptions(),
                jsonpMapper));
    }

    /**
     * The mapper gets its date configuration here, because each new transport must keep this
     * configuration.
     */
    @Bean
    @NotNull
    @Override
    public JsonpMapper jsonpMapper() {
        var jsonpMapper = super.jsonpMapper();

        if (jsonpMapper instanceof JacksonJsonpMapper jacksonMapper) {
            jacksonMapper.objectMapper().registerModule(new JavaTimeModule());
            jacksonMapper.objectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        return jsonpMapper;
    }
}
