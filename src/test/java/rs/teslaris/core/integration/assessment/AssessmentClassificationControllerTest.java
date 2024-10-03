package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.assessment.dto.AssessmentClassificationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class AssessmentClassificationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private AssessmentClassificationDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        return new AssessmentClassificationDTO(null, "rule", "code", dummyMC);
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllAssessmentClassifications() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-classification?page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAssessmentClassification() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-classification/{assessmentClassificationId}",
                    1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var assessmentClassificationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(assessmentClassificationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/assessment/assessment-classification")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_ASSESSMENT_CLASSIFICATION"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.formalDescriptionOfRule").value("rule"))
            .andExpect(jsonPath("$.code").value("code"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var assessmentClassificationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(assessmentClassificationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/assessment-classification/{assessmentClassificationId}",
                        1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/assessment/assessment-classification/{assessmentClassificationId}",
                        2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
