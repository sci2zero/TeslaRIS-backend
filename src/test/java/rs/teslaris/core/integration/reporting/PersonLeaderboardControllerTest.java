package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class PersonLeaderboardControllerTest extends BaseTest {

    @Test
    public void testGetTopResearchersByPublicationCount() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/leaderboard-data/person/publications?institutionId=1&yearFrom=2000&yearTo=2025")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetResearchersWithMostCitations() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/leaderboard-data/person/citations?institutionId=1&yearFrom=2000&yearTo=2025")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetResearchersWithMostAssessmentPoints() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/leaderboard-data/person/assessment-points?institutionId=1&yearFrom=2000&yearTo=2025")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
