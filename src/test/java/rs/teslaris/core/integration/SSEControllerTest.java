package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class SSEControllerTest extends BaseTest {

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSSEStreamingPipeline() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/sse/access-token?exportId=TEST_SSE")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/sse/progress/TEST_SSE?accessToken={accessToken}",
                        response.andReturn().getResponse().getContentAsString())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSSEStreamingPipelineNonExistingToken() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/sse/access-token?exportId=TEST_SSE_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/sse/progress/NON_EXISTING?accessToken={accessToken}",
                        response.andReturn().getResponse().getContentAsString())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testSSEStreamingPipelineUnauthorised() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        var response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/sse/access-token?exportId=TEST_SSE_2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/sse/progress/TEST_SSE_2?accessToken=WRONG_ACCESS_TOKEN",
                        response.andReturn().getResponse().getContentAsString())
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
