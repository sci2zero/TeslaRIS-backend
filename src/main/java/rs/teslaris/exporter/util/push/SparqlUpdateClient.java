package rs.teslaris.exporter.util.push;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

@Component
@Slf4j
public class SparqlUpdateClient {

    private final RestTemplateProvider restTemplateProvider;

    private final UnaryOperator<String> environment;


    // Spring Environment also contains the values from the .env file.
    @Autowired
    public SparqlUpdateClient(RestTemplateProvider restTemplateProvider,
                              Environment springEnvironment) {
        this(restTemplateProvider, springEnvironment::getProperty);
    }

    public SparqlUpdateClient(RestTemplateProvider restTemplateProvider,
                              UnaryOperator<String> environment) {
        this.restTemplateProvider = restTemplateProvider;
        this.environment = environment;
    }

    public void deleteData(PushTarget target, Collection<String> triples) {
        send(target, "DELETE DATA", triples);
    }

    public void insertData(PushTarget target, Collection<String> triples) {
        send(target, "INSERT DATA", triples);
    }

    private void send(PushTarget target, String operation, Collection<String> triples) {
        var batchSize = Math.max(1, target.batchSize());
        var batch = new ArrayList<String>(batchSize);

        for (var triple : triples) {
            batch.add(triple);
            if (batch.size() >= batchSize) {
                post(target, buildUpdate(target, operation, batch));
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            post(target, buildUpdate(target, operation, batch));
        }
    }

    private String buildUpdate(PushTarget target, String operation, List<String> triples) {
        var update = new StringBuilder(operation).append(" {\n");
        var hasGraph = Objects.nonNull(target.graph()) && !target.graph().isBlank();

        if (hasGraph) {
            update.append("GRAPH <").append(target.graph()).append("> {\n");
        }

        triples.forEach(triple -> update.append(triple).append("\n"));

        if (hasGraph) {
            update.append("}\n");
        }

        return update.append("}").toString();
    }

    private void post(PushTarget target, String update) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var form = new LinkedMultiValueMap<String, String>();
        form.add("update", update);

        var auth = target.auth();
        if (auth.type() == AuthType.VIVO_FORM) {
            form.add("email", auth.username());
            form.add("password", resolvePassword(target));
        } else if (auth.type() == AuthType.BASIC) {
            headers.setBasicAuth(auth.username(), resolvePassword(target));
        }

        var timeoutMillis = Objects.nonNull(target.requestTimeoutSeconds()) ?
            target.requestTimeoutSeconds() * 1000 : 0;

        var restTemplate = restTemplateProvider.provideRestTemplate(timeoutMillis);
        var request = new HttpEntity<>(form, headers);

        // A response that is not 2xx causes an exception, and the run stops.
        try {
            restTemplate.postForEntity(target.endpointUrl(), request, String.class);
        } catch (HttpServerErrorException e) {
            if (!isVivoStartupStatusPage(e)) {
                throw e;
            }

            log.warn("Push target '{}' returned the VIVO startup status page. Retrying once.",
                target.identifier());
            restTemplate.postForEntity(target.endpointUrl(), request, String.class);
        }
    }

    // VIVO shows this page once after a start with warnings, instead of the real response.
    private static boolean isVivoStartupStatusPage(HttpServerErrorException e) {
        return e.getStatusCode().value() == 500 &&
            e.getResponseBodyAsString().contains("<title>Startup Status</title>");
    }

    private String resolvePassword(PushTarget target) {
        var variable = target.auth().passwordEnv();
        var password = Objects.nonNull(variable) ? environment.apply(variable) : null;

        if (Objects.isNull(password) || password.isEmpty()) {
            throw new IllegalStateException(
                "Password for push target '" + target.identifier() + "' is not set. Set the " +
                    "property or environment variable named in 'passwordEnv'.");
        }

        return password;
    }
}
