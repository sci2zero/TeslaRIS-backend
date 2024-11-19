package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class DocumentClaimingControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.researcher@test.com", password = "testResearcher")
    public void testFetchPotentialClaims() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
            MockMvcRequestBuilders.get("http://localhost:8081/api/document-claim")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }
}
