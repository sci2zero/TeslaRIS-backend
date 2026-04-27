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
import rs.teslaris.core.dto.identifier.DocumentIdentifierDTO;

@SpringBootTest
public class DocumentIdentifierControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private DocumentIdentifierDTO getTestPayload(String value) {
        return new DocumentIdentifierDTO(value, 1, 10);
    }

    @Test
    public void testReadDocumentIdentifiersForDocument() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/document-identifier/{documentId}", 2)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateDocumentIdentifierSuccess() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var documentIdentifierDTO = getTestPayload("12345");

        String requestBody = objectMapper.writeValueAsString(documentIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/document-identifier/{documentId}",
                        documentIdentifierDTO.getDocumentId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_DOCUMENT_IDENTIFIER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateDocumentIdentifierWrongFormat() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var documentIdentifierDTO = getTestPayload("WRONG_FORMAT");

        String requestBody = objectMapper.writeValueAsString(documentIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/document-identifier/{documentId}",
                        documentIdentifierDTO.getDocumentId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_DOCUMENT_IDENTIFIER_2"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateDocumentIdentifier() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var documentIdentifierDTO = getTestPayload("54321");

        String requestBody = objectMapper.writeValueAsString(documentIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/document-identifier/{documentId}/{identifierId}",
                        documentIdentifierDTO.getDocumentId(), 4)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
