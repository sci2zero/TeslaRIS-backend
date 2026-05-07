package rs.teslaris.core;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.identifier.PublicationSeriesIdentifierDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class PublicationSeriesIdentifierControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private PublicationSeriesIdentifierDTO getTestPayload(String value) {
        return new PublicationSeriesIdentifierDTO(value, 1, 1);
    }

    @Test
    public void testReadPublicationSeriesIdentifiersForPublicationSeries() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/publication-series-identifier/{publicationSeriesId}", 2)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePublicationSeriesIdentifierSuccess() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publicationSeriesIdentifierDTO = getTestPayload("11111");

        String requestBody = objectMapper.writeValueAsString(publicationSeriesIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/publication-series-identifier")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PUBLICATION_SERIES_IDENTIFIER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePublicationSeriesIdentifierWrongFormat() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publicationSeriesIdentifierDTO = getTestPayload("WRONG_FORMAT");

        String requestBody = objectMapper.writeValueAsString(publicationSeriesIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/publication-series-identifier")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PUBLICATION_SERIES_IDENTIFIER_2"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdatePublicationSeriesIdentifier() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publicationSeriesIdentifierDTO = getTestPayload("11011");

        String requestBody = objectMapper.writeValueAsString(publicationSeriesIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/publication-series-identifier/{identifierId}",
                        6)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
