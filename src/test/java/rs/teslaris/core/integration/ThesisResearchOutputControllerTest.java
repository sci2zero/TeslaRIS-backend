package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ReindexRequestDTO;
import rs.teslaris.core.indexmodel.EntityType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThesisResearchOutputControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.librarian@test.com", password = "librarian")
    public void testReadThesisResearchOutput() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = new ReindexRequestDTO(List.of(EntityType.PUBLICATION));
        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/reindex")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_REINDEX1"))
            .andExpect(status().isOk());

        jwtToken = authenticateLibrarianAndGetToken();
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/thesis/research-output/{documentId}", 15)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.librarian@test.com", password = "librarian")
    public void testAddThesisResearchOutput() throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/thesis/research-output/add/{documentId}/{researchOutputId}",
                        10, 13)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.librarian@test.com", password = "librarian")
    public void testRemoveThesisResearchOutput() throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/thesis/research-output/remove/{documentId}/{researchOutputId}",
                        10, 13)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
