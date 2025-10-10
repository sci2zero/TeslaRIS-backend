package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class DocumentVisualizationDataControllerTest extends BaseTest {

    @ParameterizedTest
    @EnumSource(StatisticsType.class)
    public void testGetCountryStatisticsForDocument(StatisticsType statisticsType)
        throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/document/statistics/{documentId}/{statisticsType}?startDate=2005-02-13&endDate=2007-07-15",
                        1, statisticsType)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(StatisticsType.class)
    public void testGetMonthlyStatisticsForDocument(StatisticsType statisticsType)
        throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/document/monthly-statistics/{documentId}/{statisticsType}?startDate=2005-02-13&endDate=2007-07-15",
                        1, statisticsType)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
