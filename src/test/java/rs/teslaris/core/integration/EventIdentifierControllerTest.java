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
import rs.teslaris.core.dto.identifier.EventIdentifierDTO;

@SpringBootTest
public class EventIdentifierControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private EventIdentifierDTO getTestPayload(String value) {
        return new EventIdentifierDTO(value, 1, 1);
    }

    @Test
    public void testReadEventIdentifiersForEvent() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/event-identifier/{eventId}", 2)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateEventIdentifierSuccess() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventIdentifierDTO = getTestPayload("12345");

        String requestBody = objectMapper.writeValueAsString(eventIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/event-identifier")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_EVENT_IDENTIFIER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateEventIdentifierWrongFormat() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventIdentifierDTO = getTestPayload("WRONG_FORMAT");

        String requestBody = objectMapper.writeValueAsString(eventIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/event-identifier")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_EVENT_IDENTIFIER_2"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateEventIdentifier() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventIdentifierDTO = getTestPayload("54321");

        String requestBody = objectMapper.writeValueAsString(eventIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/event-identifier/{identifierId}",
                        2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
