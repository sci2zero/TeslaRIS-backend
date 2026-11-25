package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.person.LanguageKnowledgeRequestDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeResponseDTO;
import rs.teslaris.core.model.person.LanguageLevel;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LanguageKnowledgeControllerTest extends BaseTest {

    // The person of the testing data already knows Serbian and English, so the test takes German.
    private static final Integer GERMAN_LANGUAGE_ID = 4;

    private static final Integer FRENCH_LANGUAGE_ID = 5;

    private static Integer addedLanguageKnowledgeId;

    @Autowired
    private ObjectMapper objectMapper;


    private LanguageKnowledgeRequestDTO getTestPayload(Integer languageId, Boolean motherTongue) {
        var languageKnowledgeDTO = new LanguageKnowledgeRequestDTO();
        languageKnowledgeDTO.setLanguageId(languageId);
        languageKnowledgeDTO.setMotherTongue(motherTongue);
        languageKnowledgeDTO.setReading(LanguageLevel.C1);
        languageKnowledgeDTO.setWriting(LanguageLevel.B2);
        languageKnowledgeDTO.setOverall(LanguageLevel.B2);

        return languageKnowledgeDTO;
    }

    @Test
    @Order(1)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddLanguageKnowledge() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        String requestBody =
            objectMapper.writeValueAsString(getTestPayload(GERMAN_LANGUAGE_ID, false));

        var response = mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/language-knowledge/{personId}", 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_LANGUAGE_KNOWLEDGE"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        addedLanguageKnowledgeId =
            objectMapper.readValue(response, LanguageKnowledgeResponseDTO.class).getId();
    }

    @Test
    @Order(2)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddDuplicateLanguageKnowledgeReturnsConflict() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        String requestBody =
            objectMapper.writeValueAsString(getTestPayload(GERMAN_LANGUAGE_ID, false));

        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/language-knowledge/{personId}", 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_LANGUAGE_KNOWLEDGE_DUPLICATE"))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddSecondMotherTongueReturnsConflict() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        String requestBody =
            objectMapper.writeValueAsString(getTestPayload(FRENCH_LANGUAGE_ID, true));

        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/language-knowledge/{personId}", 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_LANGUAGE_KNOWLEDGE_MOTHER_TONGUE"))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddLanguageKnowledgeWithoutLanguageReturnsBadRequest() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        String requestBody = objectMapper.writeValueAsString(getTestPayload(null, false));

        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/language-knowledge/{personId}", 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_LANGUAGE_KNOWLEDGE_NO_LANGUAGE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateLanguageKnowledge() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var languageKnowledgeDTO = getTestPayload(GERMAN_LANGUAGE_ID, false);
        languageKnowledgeDTO.setSpeaking(LanguageLevel.A2);

        String requestBody = objectMapper.writeValueAsString(languageKnowledgeDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/language-knowledge/{personId}/{languageKnowledgeId}",
                        1, addedLanguageKnowledgeId)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteLanguageKnowledge() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/language-knowledge/{personId}/{languageKnowledgeId}",
                        1, addedLanguageKnowledgeId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
