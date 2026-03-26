package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.dto.person.PersonFieldVisibilityDTO;

@SpringBootTest
public class PersonFieldVisibilityControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testReadPublicFieldVisibilityConfiguration() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get(
                        "http://localhost:8081/api/person-field-visibility/{personId}",
                        2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test.editor@test.com", password = "testEditor")
    public void testSaveConfiguration() throws Exception {
        String jwtToken = authenticateResearcherAndGetToken();

        var configurationDTO = new PersonFieldVisibilityDTO(
            true, false,
            true, true, true, false);

        String requestBody = objectMapper.writeValueAsString(configurationDTO);
        mockMvc.perform(
                MockMvcRequestBuilders.patch(
                        "http://localhost:8081/api/person-field-visibility/{personId}",
                        2)
                    .content(requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
    }
}
