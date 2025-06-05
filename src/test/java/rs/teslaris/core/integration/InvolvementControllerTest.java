package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.model.person.EmploymentPosition;

@SpringBootTest
public class InvolvementControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testMigrateEmployment() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = new EmploymentMigrationDTO(3, 2, EmploymentPosition.ASSISTANT_PROFESSOR,
            LocalDate.of(2024, 3, 13), null, null);

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/involvement/migrate-employment")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employmentPosition").value("ASSISTANT_PROFESSOR"));
    }
}
