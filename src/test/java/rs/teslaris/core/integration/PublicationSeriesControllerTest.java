package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;

@SpringBootTest
public class PublicationSeriesControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReorderContributions() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var reorderRequest = new ReorderContributionRequestDTO(2, 1);

        String requestBody = objectMapper.writeValueAsString(reorderRequest);
        mockMvc.perform(MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/publication-series/{publicationSeriesId}/reorder-contribution/{contributionId}",
                    5, 9)
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_PUBLICATION_SERIES_REORDER"))
            .andExpect(status().isOk());
    }
}
