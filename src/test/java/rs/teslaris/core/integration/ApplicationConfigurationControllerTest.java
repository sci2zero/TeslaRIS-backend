package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class ApplicationConfigurationControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetIsApplicationInMaintenanceMode() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/app-configuration/maintenance/check")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testGetNextScheduledMaintenance() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/app-configuration/maintenance/next")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testScheduleMaintenance() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/app-configuration/maintenance/schedule?approximateEndMoment=test&timestamp=" +
                        LocalDateTime.now().plusMinutes(10))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .header("Idempotency-Key", "MOCK_KEY_MAINTENANCE"))
            .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testTurnOnMaintenanceMode() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/app-configuration/maintenance/turn-on")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/app-configuration/maintenance/turn-off")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testTurnOffMaintenanceMode() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.patch(
                    "http://localhost:8081/api/app-configuration/maintenance/turn-off")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
