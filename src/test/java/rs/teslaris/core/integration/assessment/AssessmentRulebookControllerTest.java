package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.assessment.AssessmentRulebookDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class AssessmentRulebookControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private AssessmentRulebookDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        return new AssessmentRulebookDTO(dummyMC, dummyMC,
            LocalDate.of(2023, 1, 1), null, 2, 1);
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllAssessmentRulebooks() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-rulebook?page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAssessmentRulebook() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-rulebook/{assessmentRulebookId}",
                    1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateAssessmentRulebook() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var assessmentRulebookDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(assessmentRulebookDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/assessment/assessment-rulebook")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_ASSESSMENT_RULEBOOK"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.publisherId").value(2))
            .andExpect(jsonPath("$.assessmentMeasureId").value(1));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateAssessmentRulebook() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var assessmentRulebookDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(assessmentRulebookDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/assessment-rulebook/{assessmentRulebookId}",
                        1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteAssessmentRulebook() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/assessment/assessment-rulebook/{assessmentRulebookId}",
                        2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
