package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.commontypes.CrisContextInformationDTO;
import rs.teslaris.core.dto.commontypes.IdentifierConfigurationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.document.License;

@SpringBootTest
public class CrisContextInformationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testReadConfigurationForSystem() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("http://localhost:8081/api/cris-context-information")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.toggleAssessmentModule").exists())
            .andExpect(jsonPath("$.toggleDigitalLibrary").exists())
            .andExpect(jsonPath("$.toggleDigitalRepository").exists())
            .andExpect(jsonPath("$.identifierConfiguration.PERSON.orcid.hideWhenNotSet").exists())
            .andExpect(jsonPath("$.identifierConfiguration.DOCUMENT.doi.canBeUpdated").exists())
            .andExpect(
                jsonPath("$.identifierConfiguration.ORGANISATION_UNIT.ror.featured").exists())
            .andExpect(jsonPath("$.nationalIdentifierName").exists());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfigurationForSystem() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new CrisContextInformationDTO(false, true, false, false,
            ".*", ".*", ".*", ".*", License.CC0, null, Collections.emptyList(), "", "", "", "",
            false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cris-context-information")
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

        var configurationDTO = new CrisContextInformationDTO(null, true, false, false,
            ".*", ".*", ".*", ".*", License.CC0, null, Collections.emptyList(), "", "", "", "",
            false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cris-context-information")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testSaveConfigurationForSystemUnauthorizedForResearcher() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        var configurationDTO = new CrisContextInformationDTO(false, false, false, false,
            ".*", ".*", ".*", ".*", License.CC0, null, Collections.emptyList(), "", "", "", "",
            false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cris-context-information")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveIdentifierConfigurationAndNationalIdentifierName() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new CrisContextInformationDTO(true, true, true, true,
            ".*", ".*", ".*", ".*", License.CC0,
            Map.of(ApplicableEntityType.PERSON,
                Map.of("orcid", new IdentifierConfigurationDTO(false, true, false))),
            List.of(new MultilingualContentDTO(1, "EN", "National researcher number", 1)),
            "https://example.org/researchers/{ID}", "", "", "", false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cris-context-information")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.identifierConfiguration.PERSON.orcid.featured").value("true"))
            .andExpect(
                jsonPath("$.identifierConfiguration.PERSON.orcid.canBeUpdated").value("false"))
            .andExpect(jsonPath("$.identifierConfiguration.PERSON.apvnt.canBeUpdated")
                .value("true"))
            .andExpect(jsonPath("$.nationalIdentifierName[0].content")
                .value("National researcher number"))
            .andExpect(jsonPath("$.personNationalIdUrl")
                .value("https://example.org/researchers/{ID}"));
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfigurationRejectsIdentifierOutsideTheCatalogue() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new CrisContextInformationDTO(true, true, true, true,
            ".*", ".*", ".*", ".*", License.CC0,
            Map.of(ApplicableEntityType.DOCUMENT,
                Map.of("orcid", new IdentifierConfigurationDTO(true, false, true))),
            Collections.emptyList(), "", "", "", "", false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cris-context-information")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSaveConfigurationRejectsNationalIdUrlWithoutPlaceholder() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var configurationDTO = new CrisContextInformationDTO(true, true, true, true,
            ".*", ".*", ".*", ".*", License.CC0, null, Collections.emptyList(),
            "https://example.org/researchers/", "", "", "", false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch("http://localhost:8081/api/cris-context-information")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isBadRequest());
    }
}
