package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
public class DigitalLibraryLeaderboardControllerTest extends BaseTest {

    @ParameterizedTest
    @EnumSource(StatisticsType.class)
    @WithMockUser(username = "test.librarian@test.com", password = "testLibrarian")
    public void testGetPublicationsWithMostStatisticsCount(StatisticsType statisticsType)
        throws Exception {
        String jwtToken = authenticateLibrarianAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/leaderboard-data/digital-library/statistics?institutionId=1&from=2000-01-01&to=2025-01-02&statisticsType={statisticsType}",
                        statisticsType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
