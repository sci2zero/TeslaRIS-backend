package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class PersonVisualizationDataControllerTest extends BaseTest {

    @Test
    public void testGetPublicationCountsForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/publication-count/{personId}?from=2005&to=2007",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetMCategoriesForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/m-category/{personId}?from=2005&to=2007",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetMCategoryCountsForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/m-category-count/{personId}?from=2005&to=2007",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetViewsByCountryForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/statistics/{personId}?startDate=2005-02-13&endDate=2007-07-15",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetMonthlyViewsForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/monthly-statistics/{personId}?startDate=2005-02-13&endDate=2007-07-15",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetYearlyCitationsForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/yearly-citations/{personId}?from=2005&to=2017",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetFeaturedInformationForPerson() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/person/featured/{personId}",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
