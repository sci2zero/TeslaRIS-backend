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
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PatentControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private PatentDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var patentDTO = new PatentDTO();
        patentDTO.setTitle(dummyMC);
        patentDTO.setSubTitle(dummyMC);
        patentDTO.setDescription(dummyMC);
        patentDTO.setKeywords(dummyMC);
        patentDTO.setDocumentDate("2004-11-06");

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false,
                null, null);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        patentDTO.setContributions(List.of(contribution));
        patentDTO.setUris(new HashSet<>());

        return patentDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadPatent() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/patent/{patentId}", 3)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePatent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var patentDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(patentDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/patent")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_PATENT"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2004-11-06"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdatePatent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var patentDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(patentDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/patent/{patentId}", 3)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeletePatent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/patent/{patentId}",
                        3).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadPatentByOldId() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/patent/old-id/{oldId}", 992)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
