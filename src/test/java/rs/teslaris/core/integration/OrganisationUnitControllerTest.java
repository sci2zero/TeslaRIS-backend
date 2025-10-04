package rs.teslaris.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.util.signposting.LinksetFormat;

@SpringBootTest
public class OrganisationUnitControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSearchOrganisationUnitsSimple() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/simple-search?tokens=Faculty&tokens=FTN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testReadOUForOldId() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/old-id/{organisationUnitOldId}", 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    public void testCountAll() throws Exception {
        var resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/count")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        var result = resultActions.andReturn();
        assertTrue(Long.parseLong(result.getResponse().getContentAsString()) >= 0);
    }

    @ParameterizedTest
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    @ValueSource(strings = {"AND", "OR", "NOT"})
    public void testSearchOrganisationUnitsAdvanced(String operator) throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/advanced-search?tokens=name_sr:Fakultet&tokens=" +
                            operator + "&tokens=name_sr:FTN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    public void testGetOrganisationUnitSubUnits() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                    "http://localhost:8081/api/organisation-unit/sub-units/{organisationUnitId}?page=0&size=10",
                    1)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testGetSearchFields(Boolean onlyExportFields) throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/fields?export={export}",
                        onlyExportFields)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testAddSubUnit() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.post(
                        "http://localhost:8081/api/organisation-unit-relation/{organisationUnitId}/{subUnitId}",
                        1, 2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("Idempotency-Key", "MOCK_KEY_OU_SUB_RELATION"))
            .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @EnumSource(LinksetFormat.class)
    public void testGetLinkset(LinksetFormat format) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/organisation-unit/linkset/{organisationUnitId}/{format}",
                        1, format)
                    .contentType(format.getValue()))
            .andExpect(status().isOk());
    }
}
