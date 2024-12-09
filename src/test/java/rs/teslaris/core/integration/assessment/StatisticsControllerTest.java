package rs.teslaris.core.integration.assessment;

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
public class StatisticsControllerTest extends BaseTest {

    @ParameterizedTest
    @EnumSource(value = StatisticsType.class, names = {"VIEW", "DOWNLOAD"})
    void testFetchStatisticsTypeIndicators(StatisticsType type) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/statistics/{statisticsType}", type)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void testRegisterPersonView() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/statistics/person/{personId}", 1)
                .header("Idempotency-Key", "MOCK_KEY_PERSON_STATISTICS")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
    }

    @Test
    public void testRegisterOrganisationUnitView() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/statistics/organisation-unit/{organisationUnitId}", 1)
                .header("Idempotency-Key", "MOCK_KEY_OU_STATISTICS")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
    }

    @Test
    public void testRegisterDocumentView() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.post(
                    "http://localhost:8081/api/statistics/document/{documentId}", 1)
                .header("Idempotency-Key", "MOCK_KEY_DOCUMENT_STATISTICS")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
    }
}
