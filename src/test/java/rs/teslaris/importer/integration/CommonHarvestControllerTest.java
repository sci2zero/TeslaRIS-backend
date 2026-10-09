package rs.teslaris.importer.integration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class CommonHarvestControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testScheduleMetadataEnrichmentForInstitution() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/import-common/schedule/metadata-enrichment?timestamp=" +
                            Instant.now().plus(15, ChronoUnit.MINUTES) +
                            "&recurrence=ONCE&institutionIds=1&autoload=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isAccepted());
    }
}
