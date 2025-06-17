package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.PersonPublicationSeriesContributionDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;

@SpringBootTest
public class JournalControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private PublicationSeriesDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var journalDTO = new PublicationSeriesDTO();
        journalDTO.setTitle(dummyMC);
        journalDTO.setNameAbbreviation(dummyMC);
        journalDTO.setEissn("1234-5678");
        journalDTO.setPrintISSN("1234-5678");

        var contribution =
            new PersonPublicationSeriesContributionDTO(
                PublicationSeriesContributionType.SCIENTIFIC_BOARD_MEMBER,
                LocalDate.now(), LocalDate.now());
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        journalDTO.setContributions(List.of(contribution));
        journalDTO.setLanguageTagIds(new ArrayList<>());

        return journalDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllJournals() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/journal?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadJournal() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/journal/{journalId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateJournal() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var journalDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(journalDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/journal").content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_JOURNAL")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.printISSN").value("1234-5678"))
            .andExpect(jsonPath("$.eissn").value("1234-5678"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateJournalBasic() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var journalDTO = new JournalBasicAdditionDTO();
        journalDTO.setTitle(List.of(new MultilingualContentDTO(1, "EN", "Title", 1)));
        journalDTO.setEISSN("1234-5675");
        journalDTO.setPrintISSN("1234-5675");

        String requestBody = objectMapper.writeValueAsString(journalDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/journal/basic")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_JOURNAL_BASIC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.printISSN").value("1234-5675"))
            .andExpect(jsonPath("$.eissn").value("1234-5675"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateJournal() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var journalDTO = getTestPayload();
        journalDTO.setEissn("1234-5674");
        journalDTO.setPrintISSN("1234-5674");

        String requestBody = objectMapper.writeValueAsString(journalDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/journal/{journalId}", 2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteJournal() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/journal/{journalId}", 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchJournals() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/journal/simple-search?tokens=eISSN&tokens=content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadJournalByIdentifiers() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/journal/identifier?eIssn=1234-1234&printIssn=4321-4321&openAlexId=S1234")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
