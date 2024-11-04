package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;

@SpringBootTest
public class ResearchAreaControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private ResearchAreaDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        var researchAreaDTO = new ResearchAreaDTO();
        researchAreaDTO.setName(dummyMC);
        researchAreaDTO.setDescription(dummyMC);
        researchAreaDTO.setSuperResearchAreaId(1);

        return researchAreaDTO;
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadResearchArea() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/research-area/{researchAreaId}",
                    1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateResearchArea() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var researchAreaDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(researchAreaDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/research-area")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_RESEARCH_AREA"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateResearchArea() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var researchAreaDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(researchAreaDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put("http://localhost:8081/api/research-area/{researchAreaId}",
                        2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteResearchArea() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/research-area/{researchAreaId}",
                        3).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
