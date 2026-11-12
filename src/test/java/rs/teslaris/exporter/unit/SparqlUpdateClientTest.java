package rs.teslaris.exporter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.Auth;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.AuthType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;
import rs.teslaris.exporter.util.push.SparqlUpdateClient;

@ExtendWith(MockitoExtension.class)
public class SparqlUpdateClientTest {

    private static final String URL = "http://vivo.example/api/sparqlUpdate";

    private static final Map<String, String> ENVIRONMENT = Map.of("PUSH_PASSWORD", "secret");

    @Mock
    private RestTemplateProvider restTemplateProvider;

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> requestCaptor;


    private static PushTarget target(AuthType authType, String passwordEnv) {
        return new PushTarget("vivo-local", "vivo", true, URL, "http://graph.example",
            "http://vivo.example/individual/", new Auth(authType, "admin@vivo", passwordEnv), 2,
            60, null, List.of("org_units"));
    }

    private static HttpServerErrorException serverError(String body) {
        return (HttpServerErrorException) HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY,
            body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private SparqlUpdateClient client() {
        return new SparqlUpdateClient(restTemplateProvider, ENVIRONMENT::get);
    }

    @Test
    public void shouldSendVivoFormFieldsInBatches() {
        // Given
        when(restTemplateProvider.provideRestTemplate(anyInt())).thenReturn(restTemplate);
        var triples = List.of("<a> <b> \"1\" .", "<a> <b> \"2\" .", "<a> <b> \"3\" .");

        // When
        client().insertData(target(AuthType.VIVO_FORM, "PUSH_PASSWORD"), triples);

        // Then
        verify(restTemplate, times(2)).postForEntity(eq(URL), requestCaptor.capture(),
            eq(String.class));
        var first = requestCaptor.getAllValues().getFirst().getBody();
        var update = first.getFirst("update");
        assertTrue(update.startsWith("INSERT DATA {\nGRAPH <http://graph.example> {\n"));
        assertTrue(update.contains("\"1\"") && update.contains("\"2\""));
        assertFalse(update.contains("\"3\""));
        assertEquals("admin@vivo", first.getFirst("email"));
        assertEquals("secret", first.getFirst("password"));
        assertTrue(requestCaptor.getAllValues().get(1).getBody().getFirst("update")
            .contains("\"3\""));
    }

    @Test
    public void shouldUseBasicAuthHeader() {
        // Given
        when(restTemplateProvider.provideRestTemplate(anyInt())).thenReturn(restTemplate);

        // When
        client().deleteData(target(AuthType.BASIC, "PUSH_PASSWORD"), List.of("<a> <b> <c> ."));

        // Then
        verify(restTemplate).postForEntity(eq(URL), requestCaptor.capture(), eq(String.class));
        var request = requestCaptor.getValue();
        assertTrue(request.getBody().getFirst("update").startsWith("DELETE DATA {"));
        assertTrue(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic "));
        assertNull(request.getBody().getFirst("password"));
    }

    @Test
    public void shouldNotSendWhenThereAreNoTriples() {
        // When
        client().insertData(target(AuthType.VIVO_FORM, "PUSH_PASSWORD"), List.of());

        // Then
        verifyNoInteractions(restTemplateProvider, restTemplate);
    }

    @Test
    public void shouldFailWhenPasswordIsNotSet() {
        // When / Then
        assertThrows(IllegalStateException.class,
            () -> client().insertData(target(AuthType.VIVO_FORM, "MISSING"),
                List.of("<a> <b> <c> .")));
        verifyNoInteractions(restTemplate);
    }

    @Test
    public void shouldRetryOnceWhenVivoReturnsStartupStatusPage() {
        // Given
        when(restTemplateProvider.provideRestTemplate(anyInt())).thenReturn(restTemplate);
        when(restTemplate.postForEntity(eq(URL), any(), eq(String.class)))
            .thenThrow(serverError("<html><head><title>Startup Status</title></head></html>"))
            .thenReturn(null);

        // When
        client().insertData(target(AuthType.VIVO_FORM, "PUSH_PASSWORD"), List.of("<a> <b> <c> ."));

        // Then
        verify(restTemplate, times(2)).postForEntity(eq(URL), any(), eq(String.class));
    }

    @Test
    public void shouldNotRetryOtherServerErrors() {
        // Given
        when(restTemplateProvider.provideRestTemplate(anyInt())).thenReturn(restTemplate);
        when(restTemplate.postForEntity(eq(URL), any(), eq(String.class)))
            .thenThrow(serverError("Parse error"));

        // When / Then
        assertThrows(HttpServerErrorException.class,
            () -> client().insertData(target(AuthType.VIVO_FORM, "PUSH_PASSWORD"),
                List.of("<a> <b> <c> .")));
        verify(restTemplate, times(1)).postForEntity(eq(URL), any(), eq(String.class));
    }
}
