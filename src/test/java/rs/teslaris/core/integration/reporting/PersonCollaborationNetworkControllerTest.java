package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.reporting.utility.CollaborationType;

@SpringBootTest
public class PersonCollaborationNetworkControllerTest extends BaseTest {

    @ParameterizedTest
    @EnumSource(CollaborationType.class)
    public void testGetTopResearchersByPublicationCount(CollaborationType collaborationType)
        throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/collaboration-network/{personId}?collaborationType={collaborationType}&depth=2&yearFrom=2020&yearTo=2024",
                        1, collaborationType)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(CollaborationType.class)
    public void testGetPublicationsForCollaboration(CollaborationType collaborationType)
        throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/collaboration-network/works/{sourcePersonId}/{targetPersonId}?collaborationType={collaborationType}&yearFrom=2020&yearTo=2024&page=0&size=10",
                        1, 2, collaborationType)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
