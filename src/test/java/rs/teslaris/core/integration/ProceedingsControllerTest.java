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
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.document.DocumentContributionType;

@SpringBootTest
public class ProceedingsControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private ProceedingsDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(20, "Content", 1));

        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setTitle(dummyMC);
        proceedingsDTO.setSubTitle(dummyMC);
        proceedingsDTO.setDescription(dummyMC);
        proceedingsDTO.setKeywords(dummyMC);
        proceedingsDTO.setDocumentDate("MOCK_DATE");
        proceedingsDTO.setEventId(31);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false);
        contribution.setOrderNumber(1);
        contribution.setInstitutionIds(new ArrayList<>());
        contribution.setPersonName(new PersonNameDTO());
        contribution.setContact(new ContactDTO());
        contribution.setContributionDescription(dummyMC);
        contribution.setPostalAddress(new PostalAddressDTO(16, dummyMC, dummyMC));
        contribution.setDisplayAffiliationStatement(dummyMC);
        proceedingsDTO.setContributions(List.of(contribution));
        proceedingsDTO.setUris(new HashSet<>());
        proceedingsDTO.setLanguageTagIds(new ArrayList<>());

        return proceedingsDTO;
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testReadProceedings() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/proceedings/{publicationId}", 32)
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(jsonPath("$.eisbn").value("MOCK_eISBN"));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testCreateProceedings() throws Exception {
        String jwtToken = authenticateAndGetToken();

        var proceedingsDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(proceedingsDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/proceedings")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("MOCK_DATE"));
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testUpdateProceedings() throws Exception {
        String jwtToken = authenticateAndGetToken();

        var proceedingsDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(proceedingsDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/proceedings/{publicationId}", 33)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@admin.com", password = "admin")
    public void testDeleteProceedings() throws Exception {
        String jwtToken = authenticateAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/proceedings/{publicationId}",
                        33).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
