package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.MonographPublicationType;

@SpringBootTest
public class MonographPublicationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private MonographPublicationDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var monographPublicationDTO = new MonographPublicationDTO();
        monographPublicationDTO.setMonographId(6);
        monographPublicationDTO.setTitle(dummyMC);
        monographPublicationDTO.setSubTitle(dummyMC);
        monographPublicationDTO.setDescription(dummyMC);
        monographPublicationDTO.setKeywords(dummyMC);
        monographPublicationDTO.setMonographPublicationType(MonographPublicationType.CHAPTER);
        monographPublicationDTO.setDocumentDate("2004-11-06");

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        monographPublicationDTO.setContributions(List.of(contribution));
        monographPublicationDTO.setUris(new HashSet<>());

        return monographPublicationDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadMonographPublication() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/monograph-publication/{monographPublicationId}", 8)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateMonographPublication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var monographPublicationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(monographPublicationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/monograph-publication")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_MONOGRAPH"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2004-11-06"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateMonographPublication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var monographPublicationDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(monographPublicationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/monograph-publication/{monographPublicationId}", 8)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteMonographPublication() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/monograph-publication/{monographPublicationId}",
                        9).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
