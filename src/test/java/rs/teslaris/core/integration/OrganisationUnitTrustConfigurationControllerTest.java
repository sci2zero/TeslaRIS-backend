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
import rs.teslaris.core.dto.institution.OrganisationUnitTrustConfigurationDTO;

@SpringBootTest
public class OrganisationUnitTrustConfigurationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testReadOUTrustConfiguration() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/trust-configuration/{organisationUnitId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testSaveConfiguration() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        var configurationDTO = new OrganisationUnitTrustConfigurationDTO(false, false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/organisation-unit/trust-configuration/{organisationUnitId}",
                        1)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.trustNewPublications").value("false"))
            .andExpect(jsonPath("$.trustNewDocumentFiles").value("false"));
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testValidateDocumentMetadata() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/organisation-unit/trust-configuration/validate-document-metadata/{documentId}",
                        13)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testValidateDocumentFiles() throws Exception {
        String jwtToken = authenticateInstitutionalEditorAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/organisation-unit/trust-configuration/validate-document-files/{documentId}",
                        13)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
