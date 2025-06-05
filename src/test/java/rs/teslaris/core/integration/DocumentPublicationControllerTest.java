package rs.teslaris.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ReindexRequestDTO;
import rs.teslaris.core.indexmodel.EntityType;

@SpringBootTest
public class DocumentPublicationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testCountAll() throws Exception {
        var resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/count")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        var result = resultActions.andReturn();
        assertTrue(Long.parseLong(result.getResponse().getContentAsString()) >= 0);
    }

    @Test
    public void testGetAllPublicationsForUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                "http://localhost:8081/api/document/for-researcher/{personId}", 22)
            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void testReadDocumentPublication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(
                "http://localhost:8081/api/document/{documentId}", 13)
            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testFindNonAffiliatedPublications() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/document/non-affiliated/{organisationUnitId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testUnbindResearcherFromPublication() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/document/unbind-researcher/{documentId}", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testGetResearchOutputs() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = new ReindexRequestDTO(List.of(EntityType.PUBLICATION));
        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/reindex")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_REINDEX2"))
            .andExpect(status().isOk());

        jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/document/research-output/{documentId}", 15)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testGetSearchFields(Boolean onlyExportFields) throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/fields?export={export}", onlyExportFields)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetWordCloudForSingleDocument(Boolean foreignLanguage) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/wordcloud/{documentId}?foreignLanguage={foreignLanguage}",
                        1,
                        foreignLanguage)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
