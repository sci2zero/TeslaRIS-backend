package rs.teslaris.core.integration.importer;

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
import rs.teslaris.importer.dto.LoadingConfigurationDTO;

@SpringBootTest
public class LoadingConfigurationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testReadBookSeries() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/loading-configuration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveLoadingConfiguration() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new LoadingConfigurationDTO(true, false, true);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/loading-configuration?institutionId={institutionId}", 1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
