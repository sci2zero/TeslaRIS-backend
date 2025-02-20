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
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class AssessmentMeasureControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private AssessmentMeasureDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        return new AssessmentMeasureDTO(null, "M21", "serbianPointsRulebook2025",
            "serbianScalingRulebook2025",
            dummyMC, 1);
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllAssessmentMeasures() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-measure?page=0&size=10&searchExpression=code")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadPointRules() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-measure/point-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadScalingRules() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-measure/scaling-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAssessmentGroups() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-measure/assessment-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAssessmentMeasure() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/assessment-measure/{assessmentMeasureId}",
                    1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateAssessmentMeasure() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var assessmentMeasureDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(assessmentMeasureDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/assessment/assessment-measure")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_ASSESSMENT_MEASURE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pointRule").value("serbianPointsRulebook2025"))
            .andExpect(jsonPath("$.scalingRule").value("serbianScalingRulebook2025"))
            .andExpect(jsonPath("$.code").value("M21"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateAssessmentMeasure() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var assessmentMeasureDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(assessmentMeasureDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/assessment-measure/{assessmentMeasureId}",
                        1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteAssessmentMeasure() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/assessment/assessment-measure/{assessmentMeasureId}",
                        2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
