package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.MonographType;

@SpringBootTest
public class MonographControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private MonographDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var monographDTO = new MonographDTO();
        monographDTO.setTitle(dummyMC);
        monographDTO.setSubTitle(dummyMC);
        monographDTO.setDescription(dummyMC);
        monographDTO.setKeywords(dummyMC);
        monographDTO.setLanguageTagIds(new ArrayList<>());
        monographDTO.setMonographType(MonographType.RESEARCH_MONOGRAPH);
        monographDTO.setDocumentDate("31.01.2000.");

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        monographDTO.setContributions(List.of(contribution));
        monographDTO.setUris(new HashSet<>());

        return monographDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadMonograph() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/monograph/{monographId}", 6)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateMonograph() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var monographDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(monographDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/monograph")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_MONOGRAPH"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("31.01.2000."));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateMonograph() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var monographDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(monographDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/monograph/{monographId}", 6)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteMonograph() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/monograph/{monographId}",
                        5).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
