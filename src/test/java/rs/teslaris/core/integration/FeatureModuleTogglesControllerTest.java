package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.FeatureModuleTogglesDTO;

@SpringBootTest
public class FeatureModuleTogglesControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testReadConfigurationForSystem() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/feature-module-toggles")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.toggleAssessmentModule").exists())
            .andExpect(jsonPath("$.toggleDigitalLibrary").exists())
            .andExpect(jsonPath("$.toggleDigitalRepository").exists());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfigurationForSystem() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new FeatureModuleTogglesDTO(false, true, false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/feature-module-toggles")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.toggleAssessmentModule").value("false"))
            .andExpect(jsonPath("$.toggleDigitalLibrary").value("true"))
            .andExpect(jsonPath("$.toggleDigitalRepository").value("false"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfigurationForSystemRejectsMissingToggle() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new FeatureModuleTogglesDTO(null, true, false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/feature-module-toggles")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testSaveConfigurationForSystemUnauthorizedForResearcher() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        var configurationDTO = new FeatureModuleTogglesDTO(false, false, false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/feature-module-toggles")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isUnauthorized());
    }
}
