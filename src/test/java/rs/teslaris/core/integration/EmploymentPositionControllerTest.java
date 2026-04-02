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
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;

@SpringBootTest
public class EmploymentPositionControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private EmploymentPositionDTO getTestPayload() {
        return new EmploymentPositionDTO(
            null,
            List.of(
                new MultilingualContentDTO(1, "EN", "Test", 1)
            ),
            "TEST",
            "test",
            2
        );
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateEmploymentPosition() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var employmentPositionDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(employmentPositionDTO);
        mockMvc.perform(MockMvcRequestBuilders.post("http://localhost:8081/api/employment-position")
                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_EMPLOYMENT_POSITION"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateEmploymentPosition() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var employmentPositionDTO = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(employmentPositionDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/employment-position/{employmentPositionId}",
                        2)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteEmploymentPosition() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(
                        "http://localhost:8081/api/employment-position/{employmentPositionId}",
                        3).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetChildEmploymentPositions() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/employment-position/children/{employmentPositionId}",
                        1).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchEmploymentPositions() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/employment-position/search?tokens=test&lang=sr&page=0&size=10")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
