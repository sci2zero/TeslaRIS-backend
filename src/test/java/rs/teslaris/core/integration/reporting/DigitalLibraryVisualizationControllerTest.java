package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class DigitalLibraryVisualizationControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.librarian@test.com", password = "testLibrarian")
    public void testGetPublicationsWithMostStatisticsCount()
        throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/digital-library/thesis-count/{organisationUnitId}?from=2000&to=2025",
                        1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(StatisticsType.class)
    @WithMockUser(username = "test.librarian@test.com", password = "testLibrarian")
    public void testGetPublicationsWithMostStatisticsCount(StatisticsType statisticsType)
        throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/digital-library/monthly-statistics/{organisationUnitId}?from=2000-01-01&to=2025-01-02&statisticsType={statisticsType}",
                        1, statisticsType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
