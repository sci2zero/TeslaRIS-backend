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
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.GeneticMaterialType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeneticMaterialControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private GeneticMaterialDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var geneticMaterialDTO = new GeneticMaterialDTO();
        geneticMaterialDTO.setTitle(dummyMC);
        geneticMaterialDTO.setSubTitle(dummyMC);
        geneticMaterialDTO.setDescription(dummyMC);
        geneticMaterialDTO.setKeywords(dummyMC);
        geneticMaterialDTO.setDocumentDate("2024-12-16");
        geneticMaterialDTO.setGeneticMaterialType(GeneticMaterialType.RACE);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false,
                null, null);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        geneticMaterialDTO.setContributions(List.of(contribution));
        geneticMaterialDTO.setUris(new HashSet<>());

        return geneticMaterialDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadGeneticMaterial() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/genetic-material/{geneticMaterialId}", 20)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateGeneticMaterial() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/genetic-material")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_SOFTWARE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2024-12-16"))
            .andExpect(jsonPath("$.geneticMaterialType").value("RACE"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateGeneticMaterial() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var geneticMaterialDTO = getTestPayload();
        geneticMaterialDTO.setDoi("10.1109/tsmc.2025.12345");

        String requestBody = objectMapper.writeValueAsString(geneticMaterialDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/genetic-material/{geneticMaterialId}", 20)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    @Transactional
    public void testDeleteGeneticMaterial() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/genetic-material/{geneticMaterialId}",
                        20).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
