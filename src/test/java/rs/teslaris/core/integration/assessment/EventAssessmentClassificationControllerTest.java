package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.assessment.dto.EventAssessmentClassificationDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class EventAssessmentClassificationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private EventAssessmentClassificationDTO getTestPayload() {
        var dto = new EventAssessmentClassificationDTO();
        dto.setAssessmentClassificationId(1);
        dto.setCommissionId(1);
        dto.setEventId(1);
        dto.setClassificationYear(2024);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadEventAssessmentClassificationsForEvent() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/event-assessment-classification/{eventId}", 4)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateEventAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventAssessmentClassificationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(eventAssessmentClassificationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/assessment/event-assessment-classification")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_EVENT_ASSESSMENT_CLASSIFICATION"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateEventAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var eventAssessmentClassificationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(eventAssessmentClassificationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/event-assessment-classification/{entityAssessmentClassificationId}",
                        2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
