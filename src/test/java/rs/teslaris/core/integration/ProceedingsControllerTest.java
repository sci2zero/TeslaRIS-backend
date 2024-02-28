package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;

@SpringBootTest
public class ProceedingsControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private ProceedingsDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(25, "EN", "Content", 1));

        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setTitle(dummyMC);
        proceedingsDTO.setSubTitle(dummyMC);
        proceedingsDTO.setDescription(dummyMC);
        proceedingsDTO.setKeywords(dummyMC);
        proceedingsDTO.setDocumentDate("31.01.2000.");
        proceedingsDTO.setEventId(38);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false);
        contribution.setOrderNumber(1);
        contribution.setPersonId(22);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO("Ime", "Srednje ime", "Prezime", null, null));
        proceedingsDTO.setContributions(List.of(contribution));
        proceedingsDTO.setUris(new HashSet<>());
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());

        return proceedingsDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadProceedings() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/proceedings/{publicationId}", 40)
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(jsonPath("$.eisbn").value("MOCK_eISBN"));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadProceedingsForEvent() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/proceedings/for-event/{publicationId}", 38)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreateProceedings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var proceedingsDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(proceedingsDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/proceedings")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_PROCEEDINGS"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("31.01.2000."));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdateProceedings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var proceedingsDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(proceedingsDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/proceedings/{publicationId}", 41)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteProceedings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/proceedings/{publicationId}",
                        41).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
