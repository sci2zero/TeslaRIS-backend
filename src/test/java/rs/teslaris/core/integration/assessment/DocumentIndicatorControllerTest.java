package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.assessment.dto.DocumentIndicatorDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class DocumentIndicatorControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private DocumentIndicatorDTO getTestPayload() {
        var dto = new DocumentIndicatorDTO();
        dto.setNumericValue(12d);
        dto.setFromDate(LocalDate.of(2023, 11, 10));
        dto.setToDate((LocalDate.of(2023, 12, 31)));
        dto.setIndicatorId(1);
        dto.setDocumentId(5); // monograph

        return dto;
    }

    @Test
    public void testReadDocumentIndicatorsForDocument() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/document-indicator/{documentId}", 4)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateDocumentIndicator() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var documentIndicatorDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(documentIndicatorDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/assessment/document-indicator")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_DOCUMENT_INDICATOR"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numericValue").value("12.0"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateDocumentIndicator() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var documentIndicatorDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(documentIndicatorDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/document-indicator/{entityIndicatorId}", 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
