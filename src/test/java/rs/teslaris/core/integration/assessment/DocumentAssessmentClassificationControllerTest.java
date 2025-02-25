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
import rs.teslaris.core.assessment.dto.ImaginaryJournalPublicationAssessmentRequestDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class DocumentAssessmentClassificationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadDocumentAssessmentClassificationsForDocument() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/document-assessment-classification/{documentId}",
                    1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAssessImaginaryJournalPublication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var requestPayload =
            new ImaginaryJournalPublicationAssessmentRequestDTO(1, 5, 2021, "TECHNICAL", 3);
        String requestBody = objectMapper.writeValueAsString(requestPayload);
        mockMvc.perform(
            MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/assessment/document-assessment-classification/imaginary-journal-publication")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }
}