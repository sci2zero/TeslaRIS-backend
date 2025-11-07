package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;

@SpringBootTest
public class PersonChartDisplayConfigurationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private PersonChartDisplaySettingsDTO getTestPayload() {
        return new PersonChartDisplaySettingsDTO(
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, true),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, false),
            new ChartDisplaySettings(true, true)
        );
    }

    @Test
    public void testGetChartDisplaySettingsForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/chart-display-configuration/person/{personId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSavePersonChartDisplaySettings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/chart-display-configuration/person/{organisationUnitId}",
                        1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted());
    }
}
