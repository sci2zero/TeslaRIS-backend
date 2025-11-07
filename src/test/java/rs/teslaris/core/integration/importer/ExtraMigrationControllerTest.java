package rs.teslaris.core.integration.importer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.person.InternalIdentifierMigrationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.person.EmploymentPosition;

@SpringBootTest
public class ExtraMigrationControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadBookSeries() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/extra-migration/event?oldId=2020&dateFrom=2020-01-01&dateTo=2020-01-10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testEnrichPersonInternalIdsFromExternalSource(boolean accountingIds)
        throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var payload = new InternalIdentifierMigrationDTO(Map.of(1, 1, 2, 2),
            1, LocalDate.of(2020, 1, 1), accountingIds);
        String requestBody = objectMapper.writeValueAsString(payload);

        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/extra-migration/person-internal-identifier")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCheckIfDocumentExists() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/extra-migration/check-existence/{oldId}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testMigrateEmployment() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var request = new EmploymentMigrationDTO(3, 2, EmploymentPosition.ASSISTANT_PROFESSOR,
            LocalDate.of(2024, 3, 13), null, null);

        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/extra-migration/migrate-employment")
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employmentPosition").value("ASSISTANT_PROFESSOR"));
    }
}
