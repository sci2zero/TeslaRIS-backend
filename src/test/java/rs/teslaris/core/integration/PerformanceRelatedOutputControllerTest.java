package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceRelatedOutputControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private PerformanceRelatedOutputDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var performanceRelatedOutputDTO = new PerformanceRelatedOutputDTO();
        performanceRelatedOutputDTO.setTitle(dummyMC);
        performanceRelatedOutputDTO.setSubTitle(dummyMC);
        performanceRelatedOutputDTO.setDescription(dummyMC);
        performanceRelatedOutputDTO.setKeywords(dummyMC);
        performanceRelatedOutputDTO.setDocumentDate("2003-10-07");
        performanceRelatedOutputDTO.setSourceTitle(dummyMC);
        performanceRelatedOutputDTO.setLanguageTagIds(Set.of(1, 2));
        performanceRelatedOutputDTO.setType(PerformanceRelatedOutputType.MUSICAL_PERFORMANCE);

        var contribution =
            new PersonDocumentContributionDTO(DocumentContributionType.AUTHOR, true, false, false,
                null, null);
        contribution.setOrderNumber(1);
        contribution.setPersonId(1);
        contribution.setContributionDescription(dummyMC);
        contribution.setDisplayAffiliationStatement(dummyMC);
        contribution.setPersonName(
            new PersonNameDTO(null, "Ime", "Srednje ime", "Prezime", null, null, null));
        performanceRelatedOutputDTO.setContributions(List.of(contribution));
        performanceRelatedOutputDTO.setUris(new HashSet<>());

        return performanceRelatedOutputDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadPerformanceRelatedOutput() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/performance-related-output/{performanceRelatedOutputId}",
                    21)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreatePerformanceRelatedOutput() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var performanceRelatedOutputDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(performanceRelatedOutputDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/performance-related-output")
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_PERF_REL_OUTPUT"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentDate").value("2003-10-07"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdatePerformanceRelatedOutput() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var performanceRelatedOutputDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(performanceRelatedOutputDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/performance-related-output/{performanceRelatedOutputId}",
                        21)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeletePerformanceRelatedOutput() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/performance-related-output/{performanceRelatedOutputId}",
                        21).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
