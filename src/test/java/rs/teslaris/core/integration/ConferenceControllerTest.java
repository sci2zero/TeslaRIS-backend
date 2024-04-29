package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.EventContributionType;

@SpringBootTest
public class ConferenceControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private ConferenceDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(25, "EN", "Content", 1));

        var conferenceDTO = new ConferenceDTO();
        conferenceDTO.setName(dummyMC);
        conferenceDTO.setNameAbbreviation(dummyMC);
        conferenceDTO.setState(dummyMC);
        conferenceDTO.setPlace(dummyMC);
        conferenceDTO.setDescription(dummyMC);
        conferenceDTO.setKeywords(dummyMC);
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setSerialEvent(true);
        conferenceDTO.setNumber("12R34A");
        conferenceDTO.setFee("100$");

        var contribution =
            new PersonEventContributionDTO(EventContributionType.ORGANIZATION_BOARD_CHAIR);
        contribution.setOrderNumber(1);
        contribution.setPersonId(22);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null));
        conferenceDTO.setContributions(List.of(contribution));

        return conferenceDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadAllConferences() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/conference?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadConference() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/conference/{conferenceId}", 38)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreateConference() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var conferenceDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(conferenceDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/conference")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_CONFERENCE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.number").value("12R34A"))
            .andExpect(jsonPath("$.fee").value("100$"));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreateConferenceBasic() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var date = LocalDate.now();
        var conferenceDTO = new ConferenceBasicAdditionDTO();
        conferenceDTO.setName(List.of(new MultilingualContentDTO(25, "EN", "Name", 1)));
        conferenceDTO.setDateFrom(date);
        conferenceDTO.setDateTo(date);

        String requestBody = objectMapper.writeValueAsString(conferenceDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/conference/basic")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_CONFERENCE_BASIC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dateFrom").value(date.toString()))
            .andExpect(jsonPath("$.dateTo").value(date.toString()));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdateConference() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var conferenceDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(conferenceDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/conference/{conferenceId}", 38)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteConference() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/conference/{conferenceId}",
                        46).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
