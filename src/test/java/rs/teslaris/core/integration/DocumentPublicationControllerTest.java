package rs.teslaris.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.ReindexRequestDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.document.BibliographicFormat;
import rs.teslaris.core.util.signposting.LinksetFormat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
                "http://localhost:8081/api/document/for-researcher/{personId}?tokens=*", 22)
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
                        "http://localhost:8081/api/reindex?reharvestCitationIndicators=false")
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
    @ValueSource(strings = {"SR", "SR-CYR", "EN"})
    public void testGetWordCloudForSingleDocument(String language) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/wordcloud/{documentId}?language={foreignLanguage}",
                        1, language)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testCheckDoiUsageDoiExists() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/doi-usage?doi=10.1109/tsmc.2014.2347265")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testCheckDoiUsageDoiDoesNotExist() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/doi-usage?doi=10.1109/tsmc.2014.2347264")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    @Order(Integer.MAX_VALUE - 1)
    public void testArchiveDocument() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/document/archive/{documentId}", 13)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    @Order(Integer.MAX_VALUE)
    public void testUnarchiveDocument() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/document/unarchive/{documentId}", 13)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @EnumSource(BibliographicFormat.class)
    public void testGetMetadataFormatForDocument(BibliographicFormat format) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/metadata/{documentId}/{format}", 13, format)
                    .contentType(format.getValue()))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(LinksetFormat.class)
    public void testGetLinkset(LinksetFormat format) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/document/linkset/{documentId}/{format}", 13, format)
                    .contentType(format.getValue()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testUnbindEmployedResearchersFromPublication() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/document/unbind-institution-researchers/{documentId}", 7)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
