package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class HealthCheckControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeleteRelation() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/health-check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
