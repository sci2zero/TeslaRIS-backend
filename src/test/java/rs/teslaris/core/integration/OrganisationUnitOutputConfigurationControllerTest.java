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
import rs.teslaris.core.dto.institution.OrganisationUnitOutputConfigurationDTO;

@SpringBootTest
public class OrganisationUnitOutputConfigurationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testReadOUOutputConfiguration() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/output-configuration/{organisationUnitId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testSaveConfiguration() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        var configurationDTO = new OrganisationUnitOutputConfigurationDTO(true, false, true, true);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/organisation-unit/output-configuration/{organisationUnitId}",
                        1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.showOutputs").value("true"))
            .andExpect(jsonPath("$.showBySpecifiedAffiliation").value("false"))
            .andExpect(jsonPath("$.showByPublicationYearEmployments").value("true"))
            .andExpect(jsonPath("$.showByCurrentEmployments").value("true"));
    }
}
