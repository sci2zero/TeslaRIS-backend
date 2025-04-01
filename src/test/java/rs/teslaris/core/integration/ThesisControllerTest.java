package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
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
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.ThesisType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThesisControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private ThesisDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var thesisDTO = new ThesisDTO();
        thesisDTO.setTitle(dummyMC);
        thesisDTO.setSubTitle(dummyMC);
        thesisDTO.setDescription(dummyMC);
        thesisDTO.setKeywords(dummyMC);
        thesisDTO.setDocumentDate("2004-11-06");
        thesisDTO.setThesisType(ThesisType.PHD);
        thesisDTO.setOrganisationUnitId(1);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        thesisDTO.setContributions(List.of(contribution));
        thesisDTO.setUris(new HashSet<>());

        return thesisDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadThesis() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/thesis/{thesisId}", 10)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateThesis() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var thesisDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(thesisDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/thesis")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_THESIS"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2004-11-06"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateThesis() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var thesisDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(thesisDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/thesis/{thesisId}", 10)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testPutThesisOnPublicReviewFailsForEmptyAttachmentList() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/thesis/put-on-public-review/{thesisId}", 10)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(1)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testRemoveFromPublicReviewFailsWhenNotOnPublicReview() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/thesis/remove-from-public-review/{thesisId}", 10)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(2)
    @WithMockUser(username = "test.library@test.com", password = "library")
    public void testArchiveThesis() throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/thesis/archive/{thesisId}", 10)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "test.head_of_library@test.com", password = "head_of_library")
    public void testUnarchiveThesis() throws Exception {
        String jwtToken = authenticateHeadOfLibraryAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/thesis/unarchive/{thesisId}", 10)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.head_of_library@test.com", password = "head_of_library")
    public void testGetThesisLibraryFormat() throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/thesis/library-formats/{thesisId}", 10)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteThesis() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/thesis/{thesisId}",
                        11).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
