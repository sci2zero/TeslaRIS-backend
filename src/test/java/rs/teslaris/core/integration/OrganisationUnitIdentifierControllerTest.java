package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.identifier.OrganisationUnitIdentifierDTO;

@SpringBootTest
public class OrganisationUnitIdentifierControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    private OrganisationUnitIdentifierDTO getTestPayload(String value) {
        return new OrganisationUnitIdentifierDTO(value, 1, 1);
    }

    @Test
    public void testReadOrganisationUnitIdentifiersForOrganisationUnit() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/organisation-unit-identifier/{organisationUnitId}", 1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateOrganisationUnitIdentifierSuccess() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var organisationUnitIdentifierDTO = getTestPayload("44444");

        String requestBody = objectMapper.writeValueAsString(organisationUnitIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/organisation-unit-identifier/{organisationUnitId}",
                        organisationUnitIdentifierDTO.getOrganisationUnitId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_ORGANISATION_UNIT_IDENTIFIER_1"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCreateOrganisationUnitIdentifierWrongFormat() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var organisationUnitIdentifierDTO = getTestPayload("WRONG_FORMAT");

        String requestBody = objectMapper.writeValueAsString(organisationUnitIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/organisation-unit-identifier/{organisationUnitId}",
                        organisationUnitIdentifierDTO.getOrganisationUnitId())
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_ORGANISATION_UNIT_IDENTIFIER_2"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testUpdateOrganisationUnitIdentifier() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var organisationUnitIdentifierDTO = getTestPayload("44344");

        String requestBody = objectMapper.writeValueAsString(organisationUnitIdentifierDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.put(
                        "http://localhost:8081/api/organisation-unit-identifier/{organisationUnitId}/{identifierId}",
                        organisationUnitIdentifierDTO.getOrganisationUnitId(), 5)
                    .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
