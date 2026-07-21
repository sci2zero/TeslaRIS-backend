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
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.IntellectualPropertyType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntellectualPropertyControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private IntellectualPropertyDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var intellectualPropertyDTO = new IntellectualPropertyDTO();
        intellectualPropertyDTO.setTitle(dummyMC);
        intellectualPropertyDTO.setSubTitle(dummyMC);
        intellectualPropertyDTO.setDescription(dummyMC);
        intellectualPropertyDTO.setKeywords(dummyMC);
        intellectualPropertyDTO.setDocumentDate(new FlexibleDateDTO(2004, 11, 6, null));
        intellectualPropertyDTO.setType(IntellectualPropertyType.PATENT);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false,
                null, null);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null, null));
        intellectualPropertyDTO.setContributions(List.of(contribution));
        intellectualPropertyDTO.setUris(new HashSet<>());

        return intellectualPropertyDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadIntellectualProperty() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/intellectual-property/{intellectualPropertyId}", 3)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateIntellectualProperty() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var intellectualPropertyDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(intellectualPropertyDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/intellectual-property")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PATENT"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate.year").value(2004))
            .andExpect(jsonPath("$.documentDate.month").value(11))
            .andExpect(jsonPath("$.documentDate.day").value(6));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateIntellectualProperty() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var intellectualPropertyDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(intellectualPropertyDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/intellectual-property/{intellectualPropertyId}", 3)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteIntellectualProperty() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/intellectual-property/{intellectualPropertyId}",
                        3).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadIntellectualPropertyByOldId() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/intellectual-property/old-id/{oldId}", 992)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
