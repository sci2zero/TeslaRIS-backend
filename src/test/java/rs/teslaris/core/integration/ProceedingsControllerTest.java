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
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setTitle(dummyMC);
        proceedingsDTO.setSubTitle(dummyMC);
        proceedingsDTO.setDescription(dummyMC);
        proceedingsDTO.setKeywords(dummyMC);
        proceedingsDTO.setDocumentDate("31.01.2000.");
        proceedingsDTO.setEventId(1);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        proceedingsDTO.setContributions(List.of(contribution));
        proceedingsDTO.setUris(new HashSet<>());
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());

        return proceedingsDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadProceedings() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/proceedings/{documentId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(jsonPath("$.eISBN").value("978-3-16-148410-0"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadProceedingsForBookSeries() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/proceedings/book-series/{bookSeriesId}", 2)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadProceedingsForEvent() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/proceedings/for-event/{documentId}", 1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
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
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateProceedings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var proceedingsDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(proceedingsDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/proceedings/{documentId}", 1)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteProceedings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/proceedings/{documentId}",
                        2).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
