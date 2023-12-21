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
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.PersonPublicationSeriesContributionDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.document.PublicationSeriesContributionType;

@SpringBootTest
public class JournalControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private JournalDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(25, "Content", 1));

        var journalDTO = new JournalDTO();
        journalDTO.setTitle(dummyMC);
        journalDTO.setNameAbbreviation(dummyMC);
        journalDTO.setEISSN("eISSN");
        journalDTO.setPrintISSN("printISSN");

        var contribution =
            new PersonPublicationSeriesContributionDTO(
                PublicationSeriesContributionType.SCIENTIFIC_BOARD_MEMBER,
                LocalDate.now(), LocalDate.now());
        contribution.setOrderNumber(1);
        contribution.setInstitutionIds(new ArrayList<>());
        contribution.setPersonName(new PersonNameDTO());
        contribution.setContact(new ContactDTO());
        contribution.setContributionDescription(dummyMC);
        contribution.setPostalAddress(new PostalAddressDTO(21, dummyMC, dummyMC));
        contribution.setDisplayAffiliationStatement(dummyMC);
        journalDTO.setContributions(List.of(contribution));
        journalDTO.setLanguageTagIds(new ArrayList<>());

        return journalDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadAllJournals() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/journal?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadJournal() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/journal/{journalId}", 30)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreateJournal() throws Exception {
        String jwtToken = authenticateAndGetToken();

        var journalDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(journalDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/journal").content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_JOURNAL")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.printISSN").value("printISSN"))
            .andExpect(jsonPath("$.eissn").value("eISSN"));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdateJournal() throws Exception {
        String jwtToken = authenticateAndGetToken();

        var journalDTO = getTestPayload();
        journalDTO.setEISSN("TEST_E_ISSN");
        journalDTO.setPrintISSN("TEST_PRINT_ISSN");

        String requestBody = objectMapper.writeValueAsString(journalDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/journal/{journalId}", 30)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteJournal() throws Exception {
        String jwtToken = authenticateAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/journal/{journalId}", 48)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testSearchJournals() throws Exception {
        String jwtToken = authenticateAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/journal/simple-search?tokens=eISSN&tokens=content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
