package rs.teslaris.core.integration.reporting;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class PersonCollaborationNetworkControllerTest extends BaseTest {

    @Test
    public void testGetTopResearchersByPublicationCount() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/collaboration-network/{personId}", 1)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
