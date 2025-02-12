package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.assessment.dto.EventIndicatorDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class EntityIndicatorControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private EventIndicatorDTO getTestPayload() {
        var dto = new EventIndicatorDTO();
        dto.setNumericValue(12d);
        dto.setFromDate(LocalDate.of(2023, 11, 10));
        dto.setToDate((LocalDate.of(2023, 12, 31)));
        dto.setIndicatorId(1);
        dto.setEventId(3);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteEntityIndicator() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/assessment/entity-indicator/{entityIndicatorId}",
                        4)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateEventIndicator() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventIndicatorDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(eventIndicatorDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/assessment/event-indicator/{eventId}", 2)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_EVENT_INDICATOR"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numericValue").value("12.0"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateEventIndicator() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventIndicatorDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(eventIndicatorDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/event-indicator/{eventId}/{entityIndicatorId}",
                        1, 5)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
