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
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class PublicationSeriesAssessmentClassificationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private PublicationSeriesAssessmentClassificationDTO getTestPayload() {
        var dto = new PublicationSeriesAssessmentClassificationDTO();
        dto.setAssessmentClassificationId(1);
        dto.setPublicationSeriesId(1);
        dto.setCommissionId(1);
        dto.setClassificationYear(2025);

        return dto;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadPublicationSeriesAssessmentClassificationsForPublicationSeries()
        throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/publication-series-assessment-classification/{publicationSeriesId}",
                    4)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePublicationSeriesAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publicationSeriesAssessmentClassificationDTO = getTestPayload();

        String requestBody =
            objectMapper.writeValueAsString(publicationSeriesAssessmentClassificationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/assessment/publication-series-assessment-classification")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PUB_SERIES_ASS_CLASS"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdatePublicationSeriesAssessmentClassification() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var publicationSeriesAssessmentClassificationDTO = getTestPayload();

        String requestBody =
            objectMapper.writeValueAsString(publicationSeriesAssessmentClassificationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/publication-series-assessment-classification/{entityAssessmentClassificationId}",
                        3)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
