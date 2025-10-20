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
import rs.teslaris.reporting.dto.configuration.DigitalLibraryChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.DocumentChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.FullChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.OUChartDisplaySettingsDTO;
import rs.teslaris.reporting.dto.configuration.PersonChartDisplaySettingsDTO;
import rs.teslaris.reporting.model.ChartDisplaySettings;

@SpringBootTest
public class BaseChartDisplayConfigurationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private FullChartDisplaySettingsDTO getTestPayload() {
        return new FullChartDisplaySettingsDTO(
            new PersonChartDisplaySettingsDTO(
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
            ),
            new OUChartDisplaySettingsDTO(
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
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false)
            ),
            new DocumentChartDisplaySettingsDTO(
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true)
            ),
            new DigitalLibraryChartDisplaySettingsDTO(
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, false),
                new ChartDisplaySettings(true, true),
                new ChartDisplaySettings(true, true)
            )
        );
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testGetFullChartDisplaySettings() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/chart-display-configuration/base/{organisationUnitId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveFullChartDisplaySettings() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();
        var request = getTestPayload();

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/chart-display-configuration/base/{organisationUnitId}",
                        1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted());
    }
}
