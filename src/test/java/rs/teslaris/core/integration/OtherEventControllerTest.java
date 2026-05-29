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
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.EventContributionType;
import rs.teslaris.core.model.document.OtherEventType;
import rs.teslaris.core.model.person.PersonNameType;

@SpringBootTest
public class OtherEventControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private OtherEventDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var otherEventDTO = new OtherEventDTO();
        otherEventDTO.setName(dummyMC);
        otherEventDTO.setNameAbbreviation(dummyMC);
        otherEventDTO.setCountryId(1);
        otherEventDTO.setPlace(dummyMC);
        otherEventDTO.setDescription(dummyMC);
        otherEventDTO.setKeywords(dummyMC);
        otherEventDTO.setDateFrom(LocalDate.now());
        otherEventDTO.setDateTo(LocalDate.now());
        otherEventDTO.setSerialEvent(false);
        otherEventDTO.setType(OtherEventType.LECTURE);

        var contribution =
            new PersonEventContributionDTO(EventContributionType.ORGANIZATION_BOARD_CHAIR,
                null, null, null,
                null, null, null,
                new ArrayList<>(), new ArrayList<>()
            );
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null,
                PersonNameType.DISPLAY_NAME));
        otherEventDTO.setContributions(List.of(contribution));

        return otherEventDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllOtherEvents() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/other-event?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadOtherEvent() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/other-event/{otherEventId}", 9)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateOtherEvent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var otherEventDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(otherEventDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/other-event")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_OTHER_EVENTS"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("LECTURE"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateOtherEvent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var otherEventDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(otherEventDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/other-event/{otherEventId}", 9)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteOtherEvent() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/other-event/{otherEventId}",
                        10).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
