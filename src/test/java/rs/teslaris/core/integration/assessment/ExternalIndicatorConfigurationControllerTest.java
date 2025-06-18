package rs.teslaris.core.integration.assessment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.assessment.dto.indicator.ExternalIndicatorConfigurationDTO;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class ExternalIndicatorConfigurationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadConfigurationForInstitution() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/external-indicator-configuration/institution/{organisationUnitId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    public void testReadConfigurationForDocument() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/external-indicator-configuration/document/{documentId}",
                    13)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateConfigurationForInstitution() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO =
            new ExternalIndicatorConfigurationDTO(true, false, false, true, true);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/external-indicator-configuration/{organisationUnitId}",
                        1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
