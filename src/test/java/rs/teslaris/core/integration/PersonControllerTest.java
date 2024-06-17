package rs.teslaris.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
public class PersonControllerTest extends BaseTest {

    @Test
    public void testCountAll() throws Exception {
        var resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/person/count")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        var result = resultActions.andReturn();
        assertTrue(Long.parseLong(result.getResponse().getContentAsString()) >= 0);
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testDeletePerson() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("http://localhost:8081/api/person/{personId}",
                        4).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test.admin@test.com", password = "testAdmin")
    public void testCanEditCheckAdmin() throws Exception {
        String jwtToken = authenticateAdminAndGetToken();

        mockMvc.perform(MockMvcRequestBuilders.get(
                "http://localhost:8081/api/person/{personId}/can-edit", 1)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
