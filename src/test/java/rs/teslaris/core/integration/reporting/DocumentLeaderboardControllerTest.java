package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class DocumentLeaderboardControllerTest extends BaseTest {

    @Test
    public void testGetDocumentsWithMostCitations() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/leaderboard-data/document/citations?institutionId=1&yearFrom=2000&yearTo=2025")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(StatisticsType.class)
    public void testGetPublicationsWithMostStatisticsCount(StatisticsType statisticsType)
        throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/leaderboard-data/document/statistics?institutionId=1&from=2000-01-01&to=2025-01-02&statisticsType={statisticsType}",
                        statisticsType)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
