package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class OrganisationUnitVisualizationDataControllerTest extends BaseTest {

    @Test
    public void testGetPublicationCountsForOrganisationUnit() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/organisation-unit/publication-count/{organisationUnitId}?from=2005&to=2007",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetMCategoriesForOrganisationUnit() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/organisation-unit/m-category/{organisationUnitId}?from=2005&to=2007",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetMCategoryCountsForOrganisationUnit() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/organisation-unit/m-category-count/{organisationUnitId}?from=2005&to=2007",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetViewsByCountryForOrganisationUnit() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/organisation-unit/statistics/{organisationUnitId}?startDate=2005-02-13&endDate=2007-07-15",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetMonthlyViewsForOrganisationUnit() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/visualization-data/organisation-unit/monthly-statistics/{organisationUnitId}?startDate=2005-02-13&endDate=2007-07-15",
                        1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
