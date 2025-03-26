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
import rs.teslaris.assessment.dto.ImaginaryPublicationAssessmentRequestDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;

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
            new ImaginaryPublicationAssessmentRequestDTO(1, 5, 2021, "TECHNICAL", 3, true,
                false, false, JournalPublicationType.RESEARCH_ARTICLE, null, null, null);
        String requestBody = objectMapper.writeValueAsString(requestPayload);
        mockMvc.perform(
            MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/assessment/document-assessment-classification/imaginary-journal-publication?token=everythingPasses")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAssessImaginaryProceedingsPublication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var requestPayload =
            new ImaginaryPublicationAssessmentRequestDTO(1, 5, 2021, "TECHNICAL", 3, true,
                false, false, null, ProceedingsPublicationType.REGULAR_FULL_ARTICLE, null, null);
        String requestBody = objectMapper.writeValueAsString(requestPayload);
        mockMvc.perform(
            MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/assessment/document-assessment-classification/imaginary-proceedings-publication?token=everythingPasses")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }
}