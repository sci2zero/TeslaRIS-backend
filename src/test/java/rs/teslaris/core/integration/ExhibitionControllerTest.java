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
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.EventContributionType;
import rs.teslaris.core.model.person.PersonNameType;

@SpringBootTest
public class ExhibitionControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private ExhibitionDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var exhibitionDTO = new ExhibitionDTO();
        exhibitionDTO.setName(dummyMC);
        exhibitionDTO.setNameAbbreviation(dummyMC);
        exhibitionDTO.setCountryId(1);
        exhibitionDTO.setPlace(dummyMC);
        exhibitionDTO.setDescription(dummyMC);
        exhibitionDTO.setKeywords(dummyMC);
        exhibitionDTO.setDateFrom(LocalDate.now());
        exhibitionDTO.setDateTo(LocalDate.now());
        exhibitionDTO.setSerialEvent(false);
        exhibitionDTO.setNumber("1");
        exhibitionDTO.setFee("10EUR");

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
        exhibitionDTO.setContributions(List.of(contribution));

        return exhibitionDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllExhibitions() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/exhibition?page=0&size=5")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadExhibition() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/exhibition/{exhibitionId}", 5)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateExhibition() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var exhibitionDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(exhibitionDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/exhibition")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_EXHIBITION"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.number").value("1"))
            .andExpect(jsonPath("$.fee").value("10EUR"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateExhibition() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var exhibitionDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(exhibitionDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/exhibition/{exhibitionId}", 5)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteExhibition() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/exhibition/{exhibitionId}",
                        6).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
