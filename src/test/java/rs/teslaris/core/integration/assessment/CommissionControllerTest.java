package rs.teslaris.core.integration.assessment;

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
import rs.teslaris.core.assessment.dto.CommissionDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class CommissionControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    private CommissionDTO getTestPayload() {
        var dummyMC = List.of(new MultilingualContentDTO(1, "EN", "Content", 1));

        return new CommissionDTO(null, dummyMC, List.of("source1", "source2"),
            LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), List.of(1),
            List.of(1, 2), List.of(1, 2), "load-mno", List.of("SOCIAL"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadAllCommissions() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/commission?page=0&size=10&lang=sr&onlyLoad=false&onlyClassification=false")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadCommission() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/assessment/commission/{commissionId}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateCommission() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var commissionDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(commissionDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/assessment/commission")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_COMMISSION")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.formalDescriptionOfRule").value("load-mno"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateCommission() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var commissionDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(commissionDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/assessment/commission/{commissionId}", 2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteCommission() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/assessment/commission/{commissionId}",
                        3)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
