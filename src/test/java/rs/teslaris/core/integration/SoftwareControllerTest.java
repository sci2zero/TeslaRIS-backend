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
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SoftwareControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private SoftwareDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(25, "EN", "Content", 1));

        var softwareDTO = new SoftwareDTO();
        softwareDTO.setTitle(dummyMC);
        softwareDTO.setSubTitle(dummyMC);
        softwareDTO.setDescription(dummyMC);
        softwareDTO.setKeywords(dummyMC);
        softwareDTO.setDocumentDate("31.01.2000.");

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false);
        contribution.setOrderNumber(1);
        contribution.setPersonId(22);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO("Ime", "Srednje ime", "Prezime", null, null));
        softwareDTO.setContributions(List.of(contribution));
        softwareDTO.setUris(new HashSet<>());

        return softwareDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadSoftware() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/software/{softwareId}", 116)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreateSoftware() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var softwareDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(softwareDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/software")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_SOFTWARE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("31.01.2000."));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdateSoftware() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var softwareDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(softwareDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/software/{softwareId}", 116)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteSoftware() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/software/{softwareId}",
                        116).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
